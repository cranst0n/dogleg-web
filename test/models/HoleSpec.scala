package models

import scala.io.Source

import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

import play.api.libs.json._

import test.Helpers._

object HoleSpec extends Specification with ThrownMessages {

  "Hole" should {

    "estimate yardage" in {
      Rehoboth.holes(0).estimateYardage must be equalTo(399)
    }
  }

  lazy val Rehoboth = loadCourse("Rehoboth").
    getOrElse(fail("Load course failed for 'Rehoboth'"))
}
