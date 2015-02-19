package controllers

import org.joda.time.{ DateTime, DateTimeZone }

import scaldi.Injector

import play.api.libs.json._
import play.api.mvc._

import models.{ HoleScore, Round }

import services.{ CourseDAO, CourseRatingDAO, RoundDAO }

case class RoundCreateRequest(courseId: Long, ratingId: Long, time: Long,
  holeScores: List[HoleScore], handicapOverride: Option[Int], official: Boolean)

object RoundCreateRequest {
  implicit val jsonFormat = Json.format[RoundCreateRequest]
}

class Rounds(implicit val injector: Injector) extends DoglegController with Security {

  lazy val roundDAO = inject[RoundDAO]
  lazy val courseDAO = inject[CourseDAO]
  lazy val courseRatingDAO = inject[CourseRatingDAO]

  def createRound: Action[JsValue] = HasToken(parse.json) { implicit request =>
    expect[RoundCreateRequest] { roundRequest =>

      courseDAO.findById(roundRequest.courseId).map { roundCourse =>
        courseRatingDAO.findById(roundRequest.ratingId).map { roundRating =>

          val round =
            Round(None, request.user, roundCourse, roundRating,
              new DateTime(roundRequest.time, DateTimeZone.UTC),
              roundRequest.holeScores, None, roundRequest.handicapOverride,
              roundRequest.official)

          Ok(Json.toJson(
            roundDAO.insert(round.copy(user = request.user))
          ))

        } getOrElse notFound("Unknown course rating")
      } getOrElse notFound("Unknown course")
    }
  }

  def updateRound: Action[JsValue] = HasToken(parse.json) { implicit request =>
    expect[Round] { round =>
      roundDAO.update(round).map { updatedRound =>
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
        if(roundDAO.delete(id) == 1) {
          ok("Round deleted.")
        } else {
          serverError("Failed to delete round")
        }
      } else {
        forbidden("You may only delete your own rounds.")
      }
    } getOrElse notFound("Unknown round", "Invalid ID")
  }
}
