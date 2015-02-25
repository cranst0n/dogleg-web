
import scala.concurrent.Future

import play.api.GlobalSettings
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc._
import play.api.mvc.Results._
import play.filters.gzip.GzipFilter

import scaldi.play.ScaldiSupport

import services._
import services.slick._

object Global extends WithFilters(new GzipFilter()) with GlobalSettings
  with ScaldiSupport {

  def applicationModule = ProductionModule() :: WebModule()

  override def onHandlerNotFound(request: RequestHeader): Future[Result] = {
    Future.successful(
      jsonStatus(NOT_FOUND, "Handler not found.", request.path)
    )
  }

  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {
    Future.successful(
      jsonStatus(BAD_REQUEST, error, request.path)
    )
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    Future.successful(
      jsonStatus(INTERNAL_SERVER_ERROR, ex.getMessage, request.path)
    )
  }

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
