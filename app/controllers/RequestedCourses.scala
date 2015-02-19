package controllers

import scaldi.Injector

import play.api.libs.json._
import play.api.mvc._

import models.RequestedCourse

import services.{ CourseDAO, RequestedCourseDAO, TimeSource }

case class CourseRequest(name: String, city: String, state: String,
  country: String, website: Option[String], comment: Option[String])

object CourseRequest {
  implicit val jsonFormat = Json.format[CourseRequest]
}

class RequestedCourses(implicit val injector: Injector) extends DoglegController with Security {

  lazy val timeSource = inject[TimeSource]
  lazy val requestedCourseDAO = inject[RequestedCourseDAO]
  lazy val courseDAO = inject[CourseDAO]

  def createRequest(): Action[JsValue] = HasToken(parse.json) { implicit request =>
    expect[CourseRequest] { courseRequest =>
      val toInsert = RequestedCourse(None, courseRequest.name,
        courseRequest.city, courseRequest.state, courseRequest.country,
        courseRequest.website, courseRequest.comment, timeSource.now,
        Some(request.user), None)

      Ok(Json.toJson(requestedCourseDAO.insert(toInsert)))
    }
  }

  def list(num: Int, offset: Int): Action[Unit] = HasToken(parse.empty) { implicit request =>
    Ok(Json.toJson(requestedCourseDAO.list(
      true, num.min(RequestedCourseDAO.MaxListSize), offset)))
  }

  def info(id: Long): Action[Unit] = HasToken(parse.empty) { implicit request =>
    requestedCourseDAO.findById(id).map { courseRequest =>
      Ok(Json.toJson(courseRequest))
    } getOrElse notFound("Course request not found", "Unknown ID")
  }

  def forUser(num: Int, offset: Int): Action[Unit] = HasToken(parse.empty) { implicit request =>
    request.user.id.map { userId =>
      Ok(Json.toJson(requestedCourseDAO.forUser(userId, num, offset)))
    } getOrElse notFound("Unknown user", "Unknown ID")
  }

  def fulfill(requestId: Long, courseId: Long): Action[Unit] = Admin(parse.empty) { implicit request =>
    requestedCourseDAO.findById(requestId).map { courseRequest =>
      courseDAO.findById(courseId).map { course =>
        requestedCourseDAO.update(
          requestId, courseRequest.copy(fulfilledBy = Some(course.summary))
        ).map(r => Ok(Json.toJson(r))).
          getOrElse(serverError("Request not fulfilled."))
      } getOrElse notFound("Course not found", "Unknown ID")
    } getOrElse notFound("Course request not found", "Unknown ID")
  }

  def deleteRequest(id: Long): Action[Unit] = Admin(parse.empty) { implicit request =>
    requestedCourseDAO.findById(id) map { courseRequest =>
      if(requestedCourseDAO.delete(id) == 1) {
        ok("Request deleted.")
      } else {
        serverError("Failed to delete course request.")
      }
    } getOrElse notFound("Course request not found", "Unknown ID")
  }
}
