package services.google

import scala.concurrent.Future

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.libs.ws.{ WSResponse, WS }

import models.{ GeoCode, LatLon }

import services.GeoCodingService

class GoogleGeoCodingService extends GeoCodingService {

  val geocodePrototype =
    "http://maps.googleapis.com/maps/api/geocode/json?address=%s+%s+%s&sensor=false"

  val reverseGeocodePrototype =
    "http://maps.googleapis.com/maps/api/geocode/json?latlng=%.7f,%.7f&sensor=false"

  override def geoCode(geoCode: GeoCode): Future[LatLon] = {

    val sanitized = sanitizeGeoCode(geoCode)
    val url =
      geocodePrototype.format(sanitized.city,sanitized.state,sanitized.country)

    WS.url(url).get.map(parseGeoCodeResponse).recover {
      case t: Throwable => {
        t.printStackTrace
        LatLon(0,0)
      }
    }
  }

  override def reverseGeoCode(latLon: LatLon): Future[GeoCode] = {
    val url = reverseGeocodePrototype.format(latLon.latitude,latLon.longitude)

    WS.url(url).get.map(parseReverseGeoCodeResponse).recover {
      case t: Throwable => {
        t.printStackTrace
        GeoCode("","","")
      }
    }
  }

  private[services] def sanitizeGeoCode(geoCode: GeoCode): GeoCode = {
    GeoCode(
      city = geoCode.city.replaceAll(" ", "+"),
      state = geoCode.state.replaceAll(" ", "+"),
      country = geoCode.country.replaceAll(" ", "+")
    )
  }

  private[services] def parseGeoCodeResponse(response: WSResponse) = {
    val json = response.json.as[JsObject]

    val location =
      (((json \ "results").as[JsArray].value(0)) \ "geometry" \ "location").
        as[JsObject]

    val lat = (location \ "lat").as[JsNumber].value.toDouble
    val lon = (location \ "lng").as[JsNumber].value.toDouble

    LatLon(lat,lon)
  }

  private[services] def parseReverseGeoCodeResponse(response: WSResponse) = {
    val json = response.json.as[JsObject]

    val addressStrings =
      ((json \ "results").as[JsArray] \\ "formatted_address").
        map(_.as[JsString].value).
        filter(str => addressPattern.matcher(str).matches)

    addressStrings.headOption.map { address =>
      address.split(",").toList.map(_.trim) match {
        case city :: state :: country :: tail => GeoCode(city,state,country)
        case _ => GeoCode.empty
      }
    } getOrElse GeoCode.empty
  }

  private[this] val addressPattern = """\w+, \w+, \w+""".r.pattern
}
