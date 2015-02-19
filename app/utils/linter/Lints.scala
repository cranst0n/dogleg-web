package utils.linter

import play.api.libs.json._

import models._
import Gender._

case class Lint(val message: String, val help: String)

object Lint {
  implicit val format = Json.format[Lint]
}

sealed trait LintCheck[T] extends Function[T, List[Lint]]

object CourseKmlLintCheck extends LintCheck[Course] {
  def apply(course: Course): List[Lint] = {
    (CourseNameLintCheck :: CourseNumHolesLintCheck ::
      CourseLocationLintCheck :: CourseHolesLintCheck :: Nil).
    map(_.apply(course)).flatten.filter( lint =>
      !lint.message.contains("elevation")
    )
  }
}

object CourseJsonLintCheck extends LintCheck[Course] {
  def apply(course: Course): List[Lint] = {
    (CourseKmlLintCheck :: CourseAddressLintCheck ::
      CourseRatingsLintCheck :: Nil).
    map(_.apply(course)).flatten
  }
}

object CourseNameLintCheck extends LintCheck[Course] {
  def apply(course: Course): List[Lint] = {
    course.name match {
      case "" => List(Lint(s"Course name is missing.",
        "Every course should have a name."))
      case _ => Nil
    }
  }
}

object CourseAddressLintCheck extends LintCheck[Course] {
  def apply(course: Course): List[Lint] = {
    List(
      course.city match {
        case "" => Some(Lint(s"${course.name} is missing a city.",
          "Every course should have a city."))
        case _ => None
      },
      course.state match {
        case "" => Some(Lint(s"${course.name} is missing a state.",
          "Every course should have a state."))
        case _ => None
      },
      course.country match {
        case "" => Some(Lint(s"${course.name} is missing a country.",
          "Every course should have a country."))
        case _ => None
      }
    ).flatten
  }
}

object CourseNumHolesLintCheck extends LintCheck[Course] {
  def apply(course: Course): List[Lint] = {
    course.numHoles match {
      case 9 | 18 => Nil
      case n => List(Lint(s"${course.name} has $n holes.",
        "A course should have 9 or 18 holes."))
    }
  }
}

object CourseLocationLintCheck extends LintCheck[Course] {
  def apply(course: Course): List[Lint] = {
    course.location match {
      case LatLon(lat, lon, alt) if lat > LatLon.MinLat && lat < LatLon.MaxLat &&
        lon > LatLon.MinLon && lon < LatLon.MaxLon &&
        lat.abs > 1e-5 && lon.abs > 1e-5 => {
          Nil
      }
      case _ => {
        List(
          Lint(s"${course.name} has a suspicious location: ${course.location}.",
          "Double check to make sure it's accurate."))
      }
    }
  }
}

object CourseRatingsLintCheck extends LintCheck[Course] {
  def apply(course: Course): List[Lint] = {
    course.ratings match {
      case Nil => {
        List(Lint(s"No course ratings found.",
          "A course should have at least one course rating."))
      }
      case ratings => {

        val genderLints =
          Gender.values.toList.map { gender =>
            if(ratings.exists(_.gender == gender)) {
              None
            } else {
              Some(Lint(s"${course.name} has no ${gender.toString} rating.",
              "A course should have at least one rating for each gender."))
            }
          }.flatten

        genderLints ::: course.ratings.map(CourseRatingLintCheck).flatten
      }
    }
  }
}

object CourseRatingLintCheck extends LintCheck[CourseRating] {
  def apply(rating: CourseRating): List[Lint] = {
    RatingLintCheck(rating) ::: SlopeLintCheck(rating) :::
      BogeyRatingLintCheck(rating) ::: HoleRatingsLintCheck(rating)
  }
}

object RatingLintCheck extends LintCheck[CourseRating] {
  def apply(rating: CourseRating): List[Lint] = {
    List(
      if(rating.rating >= 60 && rating.rating <= 80) {
        None
      } else {
        Some(Lint(s"Suspicious rating value for ${rating.teeName}: ${rating.rating}.",
          "Ratings are generally somewhere between 60 and 80."))
      },
      if(((rating.frontRating + rating.backRating) - rating.rating).abs < 1e-2) {
        None
      } else {
        Some(Lint(s"Suspicious front/back rating values for ${rating.teeName}: ${rating.frontRating} / ${rating.backRating}.",
         s"The front and back ratings sum should be equal to the course rating: ${rating.rating}."))
      }
    ).flatten
  }
}

object SlopeLintCheck extends LintCheck[CourseRating] {
  def apply(rating: CourseRating): List[Lint] = {
    List(
      if(rating.slope >= 55 && rating.slope <= 155) {
        None
      } else {
        Some(Lint(s"Suspicious slope value for ${rating.teeName}: ${rating.slope}.",
          "Slopes are generally somewhere between 60 and 80."))
      },
      if(((rating.frontSlope + rating.backSlope) / 2 - rating.slope).abs <= 1) {
        None
      } else {
        Some(Lint(s"Suspicious front/back slope values for ${rating.teeName}: ${rating.frontSlope} / ${rating.backSlope}.",
          s"The front and back slope mean should be +/- 1 to the rating slope: ${rating.slope}."))
      }
    ).flatten
  }
}

object BogeyRatingLintCheck extends LintCheck[CourseRating] {
  def apply(rating: CourseRating): List[Lint] = {
    rating.bogeyRating match {
      case br if br >= 75 && br <= 110 => Nil
      case _ => {
        List(Lint(s"Suspicious bogey rating value ${rating.teeName}: ${rating.bogeyRating}.",
          "Bogey ratings are generally somewhere between 75 and 110."))
      }
    }
  }
}

object HoleRatingsLintCheck extends LintCheck[CourseRating] {
  def apply(rating: CourseRating): List[Lint] = {

    val handicaps = rating.holeRatings.map(_.handicap)

    val distinctHandicaps =
      if(handicaps.distinct.size == handicaps.size) {
        None
      } else {
        val missing = (1 to handicaps.size).diff(handicaps.distinct)
        Some(Lint(s"""The ${rating.teeName} rating appears to be missing some hole handicaps: ${missing.mkString(",")}""",
          "Each hole should have a distinct handicap value."))
      }

    val (front, back) = rating.holeRatings.partition(_.number < 10)

    val frontSideHandicaps =
      if(rating.holeRatings.size == 9 || (front.forall(_.handicap % 2 == 0) || front.forall(_.handicap % 2 == 1))) {
        None
      } else {
        Some(Lint(s"The ${rating.teeName} rating appears to have mixed odd/even handicaps on the front side.",
          "Each side of the course should only have odd or even hole handicaps."))
      }

    val backSideHandicaps =
      if(rating.holeRatings.size == 9 || (back.forall(_.handicap % 2 == 0) || back.forall(_.handicap % 2 == 1))) {
        None
      } else {
        Some(Lint(s"The ${rating.teeName} rating appears to have mixed odd/even handicaps on the back side.",
          "Each side of the course should only have odd or even hole handicaps."))
      }

    val singleNineHandicaps =
      if(rating.holeRatings.size > 9 || rating.holeRatings.map(_.handicap).forall(h => h >= 1 && h <= 9)) {
        None
      } else {
        Some(Lint(s"The ${rating.teeName} rating has handicaps higher than 9 even though it's a 9 hole course.",
          "9 hole courses should have handicaps for each hole from 1 to 9."))
      }

    List(distinctHandicaps, frontSideHandicaps,
      backSideHandicaps, singleNineHandicaps).flatten :::
      rating.holeRatings.map(hr => HoleRatingLintCheck((rating, hr))).flatten
  }
}

object HoleRatingLintCheck extends LintCheck[(CourseRating, HoleRating)] {
  def apply(tuple: (CourseRating, HoleRating)): List[Lint] = {

    val (courseRating, holeRating) = tuple
    val (minFactor, maxFactor) = (0.7, 1.1)

    val (minYardage, maxYardage) = holeRating.par match {
      case 3 => (70, (Hole.Par3MaxDistance * maxFactor).toInt)
      case 4 => ((Hole.Par3MaxDistance * minFactor).toInt, (Hole.Par4MaxDistance * maxFactor).toInt)
      case 5 => ((Hole.Par4MaxDistance * minFactor).toInt, (Hole.Par5MaxDistance * maxFactor).toInt)
      case 6 => ((Hole.Par5MaxDistance * minFactor).toInt, ((Hole.Par5MaxDistance + 200) * maxFactor).toInt)
      case _ => (-1,-1)
    }

    List(
      if(holeRating.par >= 3 && holeRating.par <= 6) {
        None
      } else {
        Some(Lint(s"${courseRating.teeName} has a suspicious par value for hole ${holeRating.number}: ${holeRating.par}.",
          "Par for a hole should be between 3 and 6."))
      },
      if(holeRating.yardage >= minYardage && holeRating.yardage <= maxYardage) {
        None
      } else {
        Some(Lint(s"${courseRating.teeName} has a suspicious yardage for hole ${holeRating.number}: ${holeRating.yardage}.",
          s"Yardage for a par ${holeRating.par} hole is usually between $minYardage and $maxYardage."))
      }
    ).flatten
  }
}

object CourseHolesLintCheck extends LintCheck[Course] {
  def apply(course: Course): List[Lint] = {
    course.holes match {
      case Nil => {
        List(Lint(s"No holes found.",
          "A course should have holes that give feature information."))
      }
      case holes if holes.size == course.numHoles => {

        def go(holeList: List[Hole]): List[Lint] = {
          holeList match {
            case Nil => List()
            case h :: t => {
              HoleLintCheck(h) ++ go(t)
            }
          }
        }

        val individualLint = go(holes)

        if(course.holes.exists(hole => hole.features.exists(_.coordinates.exists(_.altitude < 1e-5)))) {
          Lint(s"Some features appear to be missing elevation data.",
            "This may indicate no elevation data has been given.") :: individualLint
        } else {
          individualLint
        }
      }
      case _ => {
        List(Lint(s"Number of holes doesn't match course 'numHoles'.",
          "Number of holes must match the 'numHoles' property of the course."))
      }
    }
  }
}

object HoleLintCheck extends LintCheck[Hole] {
  def apply(hole: Hole): List[Lint] = {
    List(
      if(hole.number < 1 || hole.number > 18) {
        Some(Lint(s"Hole has invalid number: ${hole.number}.",
          "Hole must have number from 1 to 18."))
      } else {
        None
      }
    ).flatten ::: HoleFeaturesLintCheck(hole)
  }
}

object HoleFeaturesLintCheck extends LintCheck[Hole] {
  def apply(hole: Hole): List[Lint] = {

    def featureExists(name: String): Option[Lint] = {
      hole.featureForName(name) match {
        case Some(_) => None
        case None => {
          Some(Lint(s"No $name feature for hole #${hole.number}.",
            s"A hole should always have a $name feature."))
        }
      }
    }

    List(
      if(hole.features.isEmpty) {
        Some(Lint(s"No features for hole #${hole.number}.",
                  "A hole should have at least feature for the tee and green."))
      } else {
       None
      },
      featureExists("green"),
      featureExists("tee")
    ).flatten ::: hole.features.map(f => FeatureLintCheck((hole,f))).flatten
  }
}

object FeatureLintCheck extends LintCheck[(Hole, HoleFeature)] {
  def apply(t: (Hole, HoleFeature)): List[Lint] = {
    val (hole, feature) = t

    feature.name.toLowerCase match {
      case "green" => GreenFeatureLintCheck(t)
      case "tee" => TeeFeatureLintCheck(t)
      case n if n.contains("bunker") => BunkerFeatureLintCheck(t)
      case n if !n.split(" ").exists(w => knownFeatureNames.contains(w)) => {
        List(Lint(s"Feature for hole #${hole.number} has suspicious name: ${feature.name}.",
          "Check that it's spelled correctly."))
      }
      case _ => Nil
    }
  }

  private[this] val knownFeatureNames = Set(
    "bunker", "cart", "creek", "crest", "dogleg", "fairway", "green", "lake",
    "landing", "marker", "pond", "tee", "tree", "waste", "water"
  )
}

object TeeFeatureLintCheck extends LintCheck[(Hole, HoleFeature)] {
  def apply(t: (Hole, HoleFeature)): List[Lint] = {
    val (hole, feature) = t

    feature.coordinates match {
      case List(t) => Nil
      case _ => {
        List(Lint(s"Tee feature for hole #${hole.number} has ${feature.coordinates.size} coordinates.",
          "The tee feature should always have 1 coordinate."))
      }
    }
  }
}

object GreenFeatureLintCheck extends LintCheck[(Hole, HoleFeature)] {
  def apply(t: (Hole, HoleFeature)): List[Lint] = {
    val (hole, feature) = t

    feature.coordinates match {
      case List(f, m, b) => Nil
      case _ => {
        List(Lint(s"Green feature for hole #${hole.number} has ${feature.coordinates.size} coordinates.",
          "The green feature should always have 3 coordinates."))
      }
    }
  }
}

object BunkerFeatureLintCheck extends LintCheck[(Hole, HoleFeature)] {
  def apply(t: (Hole, HoleFeature)): List[Lint] = {
    val (hole, feature) = t

    feature.coordinates match {
      case List(r, c) => Nil
      case _ => {
        List(Lint(s"Bunker feature for hole #${hole.number} has ${feature.coordinates.size} coordinates.",
          "The bunker feature should always have 2 coordinates."))
      }
    }
  }
}
