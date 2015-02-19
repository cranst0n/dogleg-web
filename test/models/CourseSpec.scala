package models

import scala.io.Source

import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

import play.api.Play
import play.api.Play.current
import play.api.libs.json._

import test.Helpers._

object CourseSpec extends Specification with ThrownMessages {

  "Course" should {

    "load from JSON file" in {
      Rehoboth.numHoles must be equalTo 18
    }

    "have location information" in {
      Rehoboth.location.latitude must be closeTo(41.867, 1e-3)
      Rehoboth.location.longitude must be closeTo(-71.256, 1e-3)
    }

    "estimate par" in {
      Rehoboth.estimatePar must be equalTo 72
    }

    "estimate yardage" in {
      Rehoboth.estimateYardage must be equalTo 6585
    }

    "retrieve holes" in {
      Rehoboth.hole(1) must beSome.which(_.number == 1)
      Rehoboth.hole(19) must beNone
    }
  }

  lazy val Rehoboth = loadCourse("Rehoboth").
    getOrElse(fail("Load course failed for 'Rehoboth'"))
}