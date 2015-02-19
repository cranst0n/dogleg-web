package services.google

import scala.concurrent.Future
import scala.concurrent.duration._

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.libs.ws.{ WSResponse, WS }

import models.LatLon

import services.ElevationService

class GoogleElevationService extends ElevationService {

  val urlPrototype =
    "http://maps.googleapis.com/maps/api/elevation/json?locations=%.12f,%.12f&sensor=false"

  // Google limits you to 5 requests per second so back off...
  override lazy val pausePerRequest = 500.millis

  override def elevation(latLon: LatLon): Future[LatLon] = {
    val url = urlPrototype.format(latLon.latitude,latLon.longitude)

    WS.url(url).get.map(parseElevationResponse(latLon,_)).recover {
      case t: Throwable => latLon
    }
  }

  private[services] def parseElevationResponse(latLon: LatLon,
    response: WSResponse) = {

    val resultJsObj =
      (response.json \ "results").as[JsArray].value(0).as[JsObject]
    val elevation = (resultJsObj \ "elevation").as[JsNumber].value.toDouble

    latLon.copy(altitude = elevation)
  }
}
