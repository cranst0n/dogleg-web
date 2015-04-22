package controllers

import scala.concurrent.Future

import akka.actor.{ Actor, Props }

import org.joda.time.{ DateTime, DateTimeZone }

import scaldi.Injector

import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.mvc._

import models.{ HoleScore, Round, User }

import services._

case class RoundCreateRequest(courseId: Long, ratingId: Long, time: Long,
  holeScores: List[HoleScore], handicapOverride: Option[Int], official: Boolean)

object RoundCreateRequest {
  implicit val jsonFormat = Json.format[RoundCreateRequest]
}

class Rounds(implicit val injector: Injector) extends DoglegController with Security {

  lazy val roundDAO = inject[RoundDAO]
  lazy val courseDAO = inject[CourseDAO]
  lazy val courseRatingDAO = inject[CourseRatingDAO]
  lazy val handicapService = inject[HandicapService]

  lazy val handicappingActor =
    Akka.system.actorOf(Props(classOf[RoundOpsActor], this), name = "roundOpsActor")

  def createRound: Action[JsValue] = HasToken(parse.json) { implicit request =>
    expect[RoundCreateRequest] { roundRequest =>

      courseDAO.findById(roundRequest.courseId).map { roundCourse =>
        courseRatingDAO.findById(roundRequest.ratingId).map { roundRating =>

          val roundTime = new DateTime(roundRequest.time, DateTimeZone.UTC)

          val round = handicapService.handicap(
            Round(None, request.user, roundCourse, roundRating,
              roundTime, roundRequest.holeScores, None,
              roundRequest.handicapOverride, roundRequest.official),
            roundDAO.before(request.user, roundTime)
          )

          handicappingActor ! UpdateHandicaps(request.user, roundTime)

          Ok(Json.toJson(roundDAO.insert(round)))

        } getOrElse notFound("Unknown course rating")
      } getOrElse notFound("Unknown course")
    }
  }

  def updateRound: Action[JsValue] = HasToken(parse.json) { implicit request =>
    expect[Round] { round =>

      val handicappedRound = handicapService.handicap(
        round, roundDAO.before(request.user, round.time))

      roundDAO.update(handicappedRound).map { updatedRound =>
        handicappingActor ! UpdateHandicaps(request.user, updatedRound.time)
        Ok(Json.toJson(updatedRound))
      } getOrElse notFound("Update failed", "Unknown round")
    }
  }

  def list(num: Int, offset: Int): Action[Unit] = HasToken(parse.empty) { implicit request =>
    Ok(Json.toJson(roundDAO.list(request.user, num.min(RoundDAO.MaxListSize), offset)))
  }

  def info(id: Long): Action[Unit] = HasToken(parse.empty) { implicit request =>
    roundDAO.findById(id).map { round =>
      Ok(Json.toJson(round))
    } getOrElse notFound("Round not found", "Unknown ID")
  }

  def deleteRound(id: Long): Action[Unit] = HasToken(parse.empty) { implicit request =>
    roundDAO.findById(id).map { round =>
      if(round.user.id == request.user.id) {
        roundDAO.delete(id).map { deletedRound =>
          handicappingActor ! UpdateHandicaps(request.user, deletedRound.time)
          ok("Round deleted.")
        } getOrElse serverError("Failed to delete round")
      } else {
        forbidden("You may only delete your own rounds.")
      }
    } getOrElse notFound("Unknown round", "Invalid ID")
  }

  def currentHandicap(slope: Double, numHoles: Int, time: Long): Action[Unit] = HasToken(parse.empty) { implicit request =>

    val handicap =
      handicapService.handicap(
        slope, numHoles, roundDAO.before(request.user, new DateTime(time))
      ).map(_.round.toInt).getOrElse(0)

    Ok(Json.obj("handicap" -> handicap))
  }

  private[this] case class UpdateHandicaps(user: User, after: DateTime)

  private[this] class RoundOpsActor extends Actor {

    lazy val userStatsService = inject[UserStatsService]

    def receive = {
      case UpdateHandicaps(user, after) => {
        roundDAO.after(user, after).map { round =>
          roundDAO.update(
            handicapService.handicap(round, roundDAO.before(user, round.time))
          )
        }
        userStatsService.update(user)
      }
    }
  }
}
