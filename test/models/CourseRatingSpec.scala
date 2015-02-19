package models

import scala.io.Source

import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

import play.api.Play
import play.api.Play.current
import play.api.libs.json._

import test.Helpers._

object CourseRatingSpec extends Specification with ThrownMessages {

  "CourseRating" should {

    "load from JSON file" in {
      Rehoboth.ratings must have size(3)
    }

    "accumulate stats" in {
      Rehoboth.ratings.find(_.teeName == "Red").map { redRating =>
        redRating.par must be equalTo 72
        redRating.yardage must be equalTo 6090
      } getOrElse fail("No Red course rating found from Rehoboth")
    }

    lazy val Rehoboth = loadCourse("Rehoboth").
      getOrElse(fail("Load course failed for 'Rehoboth'"))
  }
}