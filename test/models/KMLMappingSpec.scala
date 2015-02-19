package models

import scala.concurrent.Await
import scala.concurrent.duration._

import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

import org.junit.runner.RunWith

import scaldi.Module

import scala.xml.XML

import play.api.test._
import play.api.Play
import play.api.Play.current
import play.api.test.Helpers._

import test.Helpers._

import services._

class KMLMappingSpec extends Specification with ThrownMessages {

  "A KMLMapping" should {

    "parse from KML" in {

      val kml = KMLMapping.fromKML(testKML.toString,
        new MockGeoCodingService, new MockElevationService)

      kml.name must be equalTo("Rehoboth")
      kml.placemarks.size must be equalTo(54)

      kml.placemarks must containAllOf(
        List(
          KMLPlacemark("Home",List(LatLon(41.86726945850371,-71.25562904746407,0))),
          KMLPlacemark("1-Tee",List(LatLon(41.86717586833112,-71.25531432963095,0))),
          KMLPlacemark("1-Green",
            List(
              LatLon(41.86480180377701,-71.25256145698209,0),
              LatLon(41.86469779509792,-71.25243792793204,0),
              LatLon(41.86460415256195,-71.25232180334147,0)
            )
          )
        )
      )
    }

    "convert to a Course" in {

      val kml = KMLMapping.fromKML(testKML.toString,
        new MockGeoCodingService, new MockElevationService)
      val courseFuture = kml.toCourse

      val course = Await.result(courseFuture, Duration.Inf)

      course.holes.size must be equalTo(18)
      course.estimateYardage must be equalTo(6585)
      course.estimatePar must be equalTo(72)

      course.holes.find(_.number == 1).map {
        _.estimateYardage must be equalTo(397)
      } getOrElse fail("Couldn't find the first hole.")
    }
  }

  private[this] def testKML = {
    XML.load(getClass.getResource("/courses/Rehoboth.kml"))
  }
}