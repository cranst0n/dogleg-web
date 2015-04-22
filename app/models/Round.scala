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

  lazy val (score, netScore, putts, penaltyStrokes) =
    holeScores.foldLeft((0,0,0,0))
      { case ((score,netScore,putts,penaltyStrokes), holeScore) =>
        (score + holeScore.score,
          netScore + holeScore.netScore,
          putts + holeScore.putts,
          penaltyStrokes + holeScore.penaltyStrokes)
      }

  lazy val par = rating.par

  lazy val scoreRatings: List[(HoleScore,HoleRating)] = {
    holeScores.flatMap { holeScore =>
      rating.holeRatings.find(_.number == holeScore.hole.number).map { rating =>
        (holeScore,rating)
      }
    }
  }

  lazy val (aces, eagles, birdies, pars, bogeys, others) =
    scoreRatings.foldLeft((0,0,0,0,0,0))
      { case ((aces, eagles, birdies, pars, bogeys, others), (holeScore, holeRating)) =>

        val newAces = if(holeScore.score == 1) aces + 1 else aces
        val parDiff = holeScore.score - holeRating.par

        if(parDiff <= -2) (newAces, eagles + 1, birdies, pars, bogeys, others)
        else if(parDiff == -1) (newAces, eagles, birdies + 1, pars, bogeys, others)
        else if(parDiff == 0) (newAces, eagles, birdies, pars + 1, bogeys, others)
        else if(parDiff == 1) (newAces, eagles, birdies, pars, bogeys + 1, others)
        else (newAces, eagles, birdies, pars, bogeys, others + 1)
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
