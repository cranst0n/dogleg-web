package models

import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import org.junit.runner.RunWith

import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._

class LatLonSpec extends Specification with ThrownMessages {

  "A LatLon" should {

    "calculate distance" in {

      val dist =
        LatLon(41.86717586833112,-71.25531432963095).
          distance(LatLon(41.86469779509792,-71.25243792793204))

      dist must be closeTo(398.4,0.5)
    }

    "serialize to JSON" in {

      val json0 = Json.toJson(LatLon(1.2,3.4))
      val json1 = Json.toJson(LatLon(5.6,7.8,9.0))

      json0 must be equalTo(
        Json.obj(
          "latitude" -> 1.2,
          "longitude" -> 3.4,
          "altitude" -> 0.0
        )
      )

      json1 must be equalTo(
        Json.obj(
          "latitude" -> 5.6,
          "longitude" -> 7.8,
          "altitude" -> 9.0
        )
      )
    }

    "convert to/from VividSolutions Point" in {

      val latLon0 = LatLon(1,2,3)
      val latLon1 = LatLon(1,2)

      val point0 = LatLon.toVividPoint(latLon0)
      val point1 = LatLon.toVividPoint(latLon1)

      LatLon.fromVividPoint(point0) must be equalTo latLon0
      LatLon.fromVividPoint(point1) must be equalTo latLon1
    }
  }
}