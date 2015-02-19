package utils

import scala.concurrent.Future
import scala.concurrent.duration._

import java.util.concurrent.TimeUnit

import org.specs2.matcher.ThrownMessages
import org.specs2.mock._
import org.specs2.mutable.Specification

import models.LatLon

import services.ElevationService

import test.Helpers._

 object AddCourseElevationSpec extends Specification with ThrownMessages {

  "AddCourseElevation" should {

    "add elevation to a course" in DoglegTestApp { implicit module =>

      val mockElevationService = mock[ElevationService]
      mockElevationService.pausePerRequest returns {
        new FiniteDuration(0, TimeUnit.SECONDS)
      }

      mockElevationService.elevation(any[LatLon]) answers { x =>
        val ll = x.asInstanceOf[LatLon]
        Future.successful(ll.copy(altitude = ll.latitude))
      }

      module.bind[ElevationService] to mockElevationService

      val RehobothElevated = AddCourseElevation.toCourse(
        Rehoboth.copy(holes = Rehoboth.holes.take(2)), true)

      val allCoordinates: List[LatLon] =
        RehobothElevated.holes.flatMap(_.features.map(_.coordinates)).flatten

      allCoordinates.forall(ll => ll.altitude must be equalTo ll.latitude) must beTrue
    }
  }

  lazy val Rehoboth = loadCourse("Rehoboth").
    getOrElse(fail("Load course failed for 'Rehoboth'"))
}
