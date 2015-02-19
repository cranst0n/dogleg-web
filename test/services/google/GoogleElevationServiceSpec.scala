package services.google

import scala.io.Source

import play.api.Play
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.test.PlaySpecification

import models.{ GeoCode, LatLon }

import test.Helpers._

object GoogleElevationServiceSpec extends PlaySpecification {

  "GoogleElevationService" should {

    "parse normal elevationcode response" in DoglegTestApp { implicit module =>

      val googleService = new GoogleElevationService

      val mockedResponse = mock[WSResponse]
      mockedResponse.json returns elevationJson
      val latLon = LatLon(41.847,-71.239,0)
      val withElevation =
        googleService.parseElevationResponse(latLon,mockedResponse)

      withElevation must be equalTo(
        LatLon(41.847,-71.239,15.17218208312988)
      )
    }
  }

  lazy val elevationJson =
    loadJson("/services/google/googleElevationResponse.json")
}