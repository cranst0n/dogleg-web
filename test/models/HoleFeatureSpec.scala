package models

import scala.io.Source

import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

import play.api.libs.json._

import test.Helpers._

object HoleFeatureSpec extends Specification with ThrownMessages {

  "Hole Feature" should {

    "determine it's estimated center" in {

      val feature = HoleFeature(None, "feat",
        List(LatLon(9,3),LatLon(0,9),LatLon(3,0)))

      feature.estimateCenter must be equalTo(LatLon(4,4))
    }
  }
}