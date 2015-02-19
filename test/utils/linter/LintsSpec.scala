package utils.linter

import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

import models.LatLon

import test.Helpers._

object LintsSpec extends Specification with ThrownMessages {

  "CourseNameLintCheck" should {
    "notify of a missing name" in {
      CourseNameLintCheck(Rehoboth) must beEmpty
      CourseNameLintCheck(Rehoboth.copy(name = "")) must have size(1)
    }
  }

  "CourseAddressLintCheck" should {
    "notify of missing address components" in {
      CourseAddressLintCheck(Rehoboth) must beEmpty
      CourseAddressLintCheck(
        Rehoboth.copy(city = "", state = "", country = "")
      ) must have size(3)
    }
  }

  "CourseNumHolesLintCheck" should {
    "notify of a weird number of holes" in {
      CourseNumHolesLintCheck(Rehoboth) must beEmpty
      CourseNumHolesLintCheck(Rehoboth.copy(numHoles = 11)) must have size(1)
    }
  }

  "CourseLocationLintCheck" should {
    "notify of missing location components" in {
      CourseLocationLintCheck(Rehoboth) must beEmpty
      CourseLocationLintCheck(
        Rehoboth.copy(location = LatLon(91, -181, 0))
      ) must have size(1)
    }
  }

  "CourseRatingsLintCheck" should {
    "notify of when no rating data is given" in {
      CourseRatingsLintCheck(Rehoboth) must beEmpty
      CourseRatingsLintCheck(Rehoboth.copy(ratings = Nil)) must have size(1)
    }
  }

  "RatingLintCheck" should {
    "notify when rating values are suspect" in {

      val rating = Rehoboth.ratings.head

      RatingLintCheck(rating) must beEmpty
      RatingLintCheck(rating.copy(
        rating = 55.1
      )) must have size(2)
    }
  }

  "SlopeLintCheck" should {
    "notify when slope values are suspect" in {

      val rating = Rehoboth.ratings.head

      SlopeLintCheck(rating) must beEmpty
      SlopeLintCheck(rating.copy(
        slope = 43.2
      )) must have size(2)
    }
  }

  "BogeyRatingLintCheck" should {
    "notify when bogey rating value(s) are suspect" in {

      val rating = Rehoboth.ratings.head

      BogeyRatingLintCheck(rating) must beEmpty
      BogeyRatingLintCheck(rating.copy(
        bogeyRating = 72
      )) must have size(1)
    }
  }

  "HoleRatingsLintCheck" should {
    "notify when hole rating data is suspect" in {

      val rating = Rehoboth.ratings.head
      val holeRatings = rating.holeRatings

      HoleRatingsLintCheck(rating) must beEmpty
      HoleRatingsLintCheck(rating.copy(
        holeRatings = holeRatings(0).copy(
          handicap = holeRatings(1).handicap + 1
        ) :: holeRatings.drop(1)
      )) must have size(2)
    }
  }

  "CourseHolesLintCheck" should {
    "notify when hole data is suspect" in {

      val holes = Rehoboth.holes

      CourseHolesLintCheck(Rehoboth) must beEmpty
      CourseHolesLintCheck(Rehoboth.copy(holes = Nil)) must have size(1)
    }
  }

  "HoleLintCheck" should {
    "notify when hole data is suspect" in {

      val holes = Rehoboth.holes

      HoleLintCheck(holes(0)) must beEmpty
      HoleLintCheck(holes(0).copy(number = -1)) must have size(1)
    }
  }

  "HoleFeaturesLintCheck" should {
    "notify when feature data is suspect" in {

      val hole = Rehoboth.holes(0)

      HoleFeaturesLintCheck(hole) must beEmpty
      HoleFeaturesLintCheck(hole.copy(features = Nil)) must have size(3)
    }
  }

  "FeatureLintCheck" should {
    "notify when feature data is suspect" in {

      val hole = Rehoboth.holes(0)
      val feature = hole.features(0)

      FeatureLintCheck((hole, feature)) must beEmpty
      FeatureLintCheck((hole, feature.copy(
        name = "bogus name",
        coordinates = List(LatLon(2,2,2))
      ))) must have size(1)

      FeatureLintCheck((hole, feature.copy(
        name = "green",
        coordinates = List(LatLon(2,2,2))
      ))) must have size(1)

      FeatureLintCheck((hole, feature.copy(
        name = "tee",
        coordinates = List(LatLon(2, 2, 2), LatLon(2, 2, 2))
      ))) must have size(1)

      FeatureLintCheck((hole, feature.copy(
        name = "bunker",
        coordinates = List(LatLon(2, 2, 2))
      ))) must have size(1)

    }
  }

  lazy val Rehoboth = loadCourse("Rehoboth").
    getOrElse(fail("Load course failed for 'Rehoboth'"))
}