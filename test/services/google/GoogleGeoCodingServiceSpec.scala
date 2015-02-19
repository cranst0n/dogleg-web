package services.google

import scala.io.Source

import play.api.Play
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.test.PlaySpecification

import models.{ GeoCode, LatLon }

import test.Helpers._

object GoogleGeoCodingServiceSpec extends PlaySpecification {

  "GoogleGeoCodingService" should {

    "parse normal geo code response" in DoglegTestApp { implicit module =>

      val googleService = new GoogleGeoCodingService

      val mockedResponse = mock[WSResponse]
      mockedResponse.json returns geoCodeJson
      val latLon = googleService.parseGeoCodeResponse(mockedResponse)

      latLon must be equalTo(LatLon(41.8470056,-71.2393933))
    }

    "parse normal reverse geo code response" in DoglegTestApp { implicit module =>

      val googleService = new GoogleGeoCodingService

      val mockedResponse = mock[WSResponse]
      mockedResponse.json returns reverseGeoCodeJson
      val geoCode = googleService.parseReverseGeoCodeResponse(mockedResponse)

      geoCode must be equalTo(GeoCode("Rehoboth","MA","USA"))
    }

    "sanitize GeoCode to put into Google URL" in {

      val googleService = new GoogleGeoCodingService

      val gc = GeoCode("New York","New York","USA")
      googleService.sanitizeGeoCode(gc) must be equalTo(
        GeoCode("New+York","New+York","USA")
      )
    }
  }

  lazy val geoCodeJson =
    loadJson("/services/google/googleGeoCodeResponse.json")

  lazy val reverseGeoCodeJson =
    loadJson("/services/google/googleReverseGeoCodeResponse.json")
}
