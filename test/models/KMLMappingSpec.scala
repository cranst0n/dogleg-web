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

      val kml = KMLMapping(testKML.toString)

      kml.name must be equalTo("Rehoboth")
      kml.placemarks.size must be equalTo(72)

      kml.placemarks must containAllOf(
        List(
          KMLPlacemark("Home",List(LatLon(41.86710794052525,-71.25602432692571,27.3316535949707))),
          KMLPlacemark("1-Tee",List(LatLon(41.86717586833112,-71.25531432963095,28.32705688476562))),
          KMLPlacemark("1-Green",
            List(
              LatLon(41.86480180377701,-71.2525614569821,25.25018501281738),
              LatLon(41.86469779509792,-71.25243792793204,26.53951644897461),
              LatLon(41.86460415256195,-71.25232180334147,26.71620750427246)
            )
          ),
          KMLPlacemark("1-Flyby",
            List(
              LatLon(41.86716933529569,-71.25528286626346,28.30869674682617),
              LatLon(41.86546673289666,-71.25361290439439,26.8432731628418),
              LatLon(41.86469766993296,-71.25242525232228,26.55384063720703)
            )
          )
        )
      )
    }

    "convert to a Course" in {

      val kml = KMLMapping(testKML.toString)
      val courseFuture =
        kml.toCourse(new MockGeoCodingService, new MockElevationService)

      val course = Await.result(courseFuture, Duration.Inf)

      course.holes.size must be equalTo(18)
      course.estimateYardage must be equalTo(6607)
      course.estimatePar must be equalTo(72)

      course.holes.find(_.number == 1).map {
        _.estimateYardage must be equalTo(399)
      } getOrElse fail("Couldn't find the first hole.")
    }
  }

  private[this] def testKML = {
    XML.load(getClass.getResource("/courses/Rehoboth.kml"))
  }
}
