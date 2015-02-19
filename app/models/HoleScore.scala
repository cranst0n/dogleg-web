package models

import play.api.libs.json.Json

case class HoleScore(roundId: Option[Long], score: Int, netScore: Int, putts: Int,
  penaltyStrokes: Int, fairwayHit: Boolean, gir: Boolean, hole: Hole)

object HoleScore {
  implicit val jsonFormat = Json.format[HoleScore]
}
