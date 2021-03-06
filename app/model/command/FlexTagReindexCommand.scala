package model.command

import com.gu.tagmanagement.{OperationType, TaggingOperation}
import model.Tag
import play.api.Logger
import repositories._
import services.{Contexts, KinesisStreams}

import scala.concurrent.Future

case class FlexTagReindexCommand(tag: Tag) extends Command {

  type T = Tag

  override def process()(implicit username: Option[String] = None): Future[Option[T]] = {
    Future {
      val contentIds = ContentAPI.getContentIdsForTag(tag.path)

      contentIds foreach { contentPath =>
        val taggingOperation = TaggingOperation(
          operation = OperationType.Reindex,
          contentPath = contentPath
        )
        Logger.info(s"raising flex reindex for content $contentPath")
        KinesisStreams.taggingOperationsReIndexStream.publishUpdate(contentPath.take(200), taggingOperation)
      }
      Some(tag)
    }(Contexts.tagOperationContext)
  }
}
