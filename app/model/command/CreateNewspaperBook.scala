package model.command

import com.gu.tagmanagement.{EventType, SectionEvent, TagEvent}
import org.cvogt.play.json.Jsonx
import org.joda.time.{DateTime, DateTimeZone}
import repositories._
import CommandError._
import model.command.logic.TagPathCalculator
import model.{PodcastMetadata, Reference, Tag, TagAudit}
import services.{Contexts, KinesisStreams}

import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
case class CreateNewspaperBook(
                                internalName: String,
                                externalName: String,
                                slug: String,
                                preCalculatedPath: String,

                                publication: Long,

                                section: Long,
                                capiSectionId: String)
  extends Command {

  type T = Tag

  def process()(implicit username: Option[String] = None): Future[Option[Tag]] = Future {

    val tagId = Sequences.tagId.getNextId

    val sectionId = Some(section)

    val pageId = try { PathManager.registerPathAndGetPageId(preCalculatedPath) } catch { case p: PathRegistrationFailed => PathInUse}

    val tag = Tag(
      id = tagId,
      path = preCalculatedPath,
      pageId = pageId,
      internalName = internalName,
      externalName = externalName,
      slug = slug,
      section = sectionId,
      capiSectionId = Some(capiSectionId),
      publication = Some(publication),

      // Hardcoded
      `type`= "NewspaperBook",
      comparableValue = externalName.toLowerCase(),
      parents = Set(),
      hidden = false,
      legallySensitive = false,
      categories = Set(),
      description = None,
      externalReferences = Nil,
      podcastMetadata = None,
      contributorInformation = None,
      publicationInformation = None,
      isMicrosite = false,
      trackingInformation = None,
      activeSponsorships = Nil,
      sponsorship = None,
      paidContentInformation = None,
      expired = false,
      updatedAt = new DateTime(DateTimeZone.UTC).getMillis
    )

    val result = TagRepository.upsertTag(tag)

    KinesisStreams.tagUpdateStream.publishUpdate(tag.id.toString, TagEvent(EventType.Update, tag.id, Some(tag.asThrift)))

    TagAuditRepository.upsertTagAudit(TagAudit.created(tag))

    result
  }
}

object CreateNewspaperBook {

  implicit val createNewspaperBookCommandFormat = Jsonx.formatCaseClassUseDefaults[CreateNewspaperBook]
}