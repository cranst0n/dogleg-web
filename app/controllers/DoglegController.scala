package controllers

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.json._

trait DoglegController extends Controller {

  def expect[T](f: (T) => Result)
    (implicit request: Request[JsValue], fmt: Format[T]): Result = {

    request.body.validate[T] match {
      case JsSuccess(item, _) => f(item)
      case JsError(errors) => {
        badRequest("JSON validation error.", errors.mkString)
      }
    }
  }

  def expectAsync[T](f: (T) => Future[Result])
    (implicit request: Request[JsValue], fmt: Format[T]): Future[Result] = {

    request.body.validate[T] match {
      case JsSuccess(item, _) => f(item)
      case JsError(errors) => {
        Future.successful(badRequest("JSON validation error.", errors.mkString))
      }
    }
  }

  def ok(message: String, details: String = ""): Result =
    jsonStatus(OK, message, details)

  def badRequest(message: String, details: String = ""): Result =
    jsonStatus(BAD_REQUEST, message, details)

  def forbidden(message: String, details: String = ""): Result =
    jsonStatus(FORBIDDEN, message, details)

  def notFound(message: String, details: String = ""): Result =
    jsonStatus(NOT_FOUND, message, details)

  def unauthorized(message: String, details: String = ""): Result =
    jsonStatus(UNAUTHORIZED, message, details)

  def serverError(message: String, details: String = ""): Result =
    jsonStatus(INTERNAL_SERVER_ERROR, message, details)

  private[this] def jsonStatus(status: Int, message: String, details: String = "") = {
    Status(status)(
      Json.obj(
        "status" -> status,
        "message" -> message,
        "details" -> details
      )
    )
  }
}
