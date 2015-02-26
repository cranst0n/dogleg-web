package models

import play.api.libs.json.Json

case class HoleScore(id: Option[Long], roundId: Option[Long], score: Int,
  netScore: Int, putts: Int, penaltyStrokes: Int, fairwayHit: Boolean,
  gir: Boolean, hole: Hole)

object HoleScore {
  implicit val jsonFormat = Json.format[HoleScore]

  implicit object HoleScoreOrdering extends scala.math.Ordering[HoleScore] {
    def compare(a: HoleScore, b: HoleScore): Int = {
      a.hole.number - b.hole.number
    }
  }
}
