package model.jobs

import com.amazonaws.services.dynamodbv2.document.Item
import play.api.libs.json._
import play.api.libs.functional.syntax._
import org.cvogt.play.json.Jsonx
import model.jobs.steps._
import model.{AppAudit, Tag, TagAudit}
import repositories._
import org.joda.time.{DateTime, DateTimeZone}
import scala.util.control.NonFatal

case class Job(
  id: Long, // Useful so users can report job failures
  steps: List[Step], // What are the steps in this job

  lockedAt: Long = 0,
  ownedBy: Option[String] = None, // Which node current owns this job
  var jobStatus: String = JobStatus.waiting, // Waiting, Owned, Failed, Complete
  var retries: Int = 0, // How many times have we had to retry a check
  var waitUntil: Long = new DateTime(DateTimeZone.UTC).getMillis, // Signal to the runner to wait until a given time before processing
  createdAt: Long = new DateTime().getMillis // Created at in local time
) {

  val retryLimit = 10 // TODO tune

  /** Process the current step of a job
   *  returns a bool which tells the job runner to requeue the job in dynamo
   *  or simply continue processing. */
  def processStep() = {
    steps.find(_.stepStatus != StepStatus.complete).foreach { step =>
      step.stepStatus match {
        case StepStatus.ready => {
          retries = 0
          step.stepStatus = StepStatus.processing
          step.process
          step.stepStatus = StepStatus.processed
        }

        case StepStatus.processed => {
          if (retries >= retryLimit) {
            throw new TooManyAttempts("Took too many attempts to process step")
          }

          if (step.check) {
            step.stepStatus = StepStatus.complete
          } else {
            retries = retries + 1
          }
        }
        case _ => {}
      }

      waitUntil = new DateTime(DateTimeZone.UTC).getMillis() + step.waitDuration.map(_.toMillis).getOrElse(0L)
    }
  }

  def rollback = {
    // Undo in reverse order
    val revSteps = steps.reverse
    revSteps
      .filter(s => s.stepStatus == StepStatus.processing || s.stepStatus == StepStatus.processed || s.stepStatus == StepStatus.complete )
      .foreach(step => {
        try {
          step.rollback
          step.stepStatus = StepStatus.rolledback
        } catch {
          case NonFatal(e) => step.stepStatus = StepStatus.rollbackfailed
        }
      })
    jobStatus = JobStatus.failed
  }

  def toItem = Item.fromJSON(Json.toJson(this).toString())
}

object Job {
  implicit val jobFormat: Format[Job] = Jsonx.formatCaseClassUseDefaults[Job]

  def fromItem(item: Item): Job = try {
      Json.parse(item.toJSON).as[Job]
    } catch {
      case NonFatal(e) => {
        println(e.printStackTrace())
        throw e
    }
  }
}

/** The job status is used to indicate if a job can be picked up off by a node as well as indicating progress
 *  to clients.
 */
object JobStatus {
  /** This job is waiting to be serviced */
  val waiting  = "waiting"

  /** This job is owned by a node */
  val owned    = "owned"

  /** This job is complete */
  val complete = "complete"

  /** This job has failed */
  val failed   = "failed"
}

case class TooManyAttempts(message: String) extends RuntimeException(message)
