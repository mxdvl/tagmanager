package model.jobs.steps

import model.{Section, Tag}
import org.cvogt.play.json.Jsonx
import repositories._
import play.api.Logger
import play.api.libs.json._
import services.{Config, KinesisStreams}
import scala.util.control.NonFatal
import scala.concurrent.duration._
import model.jobs.{Step, StepStatus}

case class ReindexTags(`type`: String = ReindexTags.`type`, var stepStatus: String = StepStatus.ready) extends Step {
  override def process = {
    val total = TagLookupCache.allTags.get.size
    var progress: Int = 0

    try {
      TagRepository.loadAllTags.grouped(Config().reindexTagsBatchSize).foreach { tags =>
        KinesisStreams.reindexTagsStream.publishUpdate("tagReindex", Tag.createReindexBatch(tags.toList))

        progress += tags.size
        ReindexProgressRepository.updateTagReindexProgress(progress, total)
      }
      ReindexProgressRepository.completeTagReindex(progress, total)
    } catch {
      case NonFatal(e) => {
        Logger.error("Tag reindex failed", e)
        ReindexProgressRepository.failTagReindex(progress, total)

        // We need to rethrow the failure to make sure the jobrunner is aware we failed
        throw e
      }
    }
  }

  override def waitDuration: Option[Duration] = {
    None
  }

  override def check: Boolean = {
    true
  }

  override def rollback = {
    throw new UnsupportedOperationException("Rollback is not supported for reindexing tags.")
  }

  override def failureMessage = s"Failed to reindex tags."
}

object ReindexTags {
  val `type` = "reindex-tags"
}
