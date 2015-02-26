package models

import org.joda.time.DateTime

import play.api.libs.json.Json

case class Round(id: Option[Long], user: User, course: Course,
  rating: CourseRating, time: DateTime, holeScores: List[HoleScore],
  handicap: Option[Int], handicapOverride: Option[Int], official: Boolean) {

  lazy val numHoles = holeScores.size

  lazy val (fullRound,front9,back9) = {
    val holeNumbers = holeScores.map(_.hole.number)

    (holeScores.size == 18,
      holeScores.size == 9 && holeNumbers.min == 1,
      holeScores.size == 9 && holeNumbers.min == 9)
  }

  lazy val handicapDifferential: Double = {
    (fullRound, front9, back9) match {
      case (true,_,_) => ((score - rating.rating) * 113) / rating.slope
      case (_,true,_) => ((score - rating.frontRating) * 113) / rating.frontSlope
      case _ => ((score - rating.backRating) * 113) / rating.backSlope
    }
  }

  lazy val (score,putts,penaltyStrokes) =
    holeScores.foldLeft((0,0,0)) { case ((score,putts,penaltyStrokes),hole) =>
      (score + hole.score, putts + hole.putts,
        penaltyStrokes + hole.penaltyStrokes)
    }

  lazy val par = rating.par

  lazy val scoreRatings: List[(HoleScore,HoleRating)] = {
    holeScores.flatMap { holeScore =>
      rating.holeRatings.find(_.number == holeScore.hole.number).map { rating =>
        (holeScore,rating)
      }
    }
  }
}

object Round {
  implicit val jsonFormat = Json.format[Round]

  implicit object RoundOrdering extends scala.math.Ordering[Round] {
    def compare(a: Round, b: Round): Int = {
      (b.time.getMillis - a.time.getMillis).toInt
    }
  }
}
