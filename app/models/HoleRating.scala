package models

import play.api.libs.json.Json

case class HoleRating(id: Option[Long], number: Int, par: Int, yardage: Int,
  handicap: Int)

object HoleRating {
  implicit val jsonFormat = Json.format[HoleRating]

  implicit object HoleRatingOrdering extends scala.math.Ordering[HoleRating] {
    def compare(a: HoleRating, b: HoleRating): Int = {
      a.number - b.number
    }
  }
}
