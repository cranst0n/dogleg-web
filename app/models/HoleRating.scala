package models

import play.api.libs.json.Json

case class HoleRating(number: Int, par: Int, yardage: Int, handicap: Int)

object HoleRating {
  implicit val jsonFormat = Json.format[HoleRating]
}
