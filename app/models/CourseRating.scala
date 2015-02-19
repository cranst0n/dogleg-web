package models

import play.api.libs.json.Json

import Gender._

import utils.json.JsonUtils._

case class CourseRating(id: Option[Long], teeName: String, rating: Double,
  slope: Double, frontRating: Double, frontSlope: Double, backRating: Double,
  backSlope: Double, bogeyRating: Double, gender: Gender, holeRatings: List[HoleRating]) {

  lazy val par = holeRatings.map(_.par).sum

  lazy val yardage = holeRatings.map(_.yardage).sum
}

object CourseRating {
  implicit val jsonFormat = Json.format[CourseRating]

  implicit object CourseRatingOrdering extends scala.math.Ordering[CourseRating] {
    def compare(a: CourseRating, b: CourseRating): Int = {
      (a,b) match {
        case (a,b) if a.gender != b.gender && a.gender == Female => 1
        case (a,b) if a.gender != b.gender && a.gender == Male => -1
        case (a,b) if a.bogeyRating > b.bogeyRating => 1
        case _ => 0
      }
    }
  }
}
