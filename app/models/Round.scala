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

  private[this] lazy val (grossScoreStats, netScoreStats) =
    scoreRatings.foldLeft(ScoreStats(0,0,0,0,0,0),ScoreStats(0,0,0,0,0,0))
      { case ((grossStats, netStats), (holeScore, holeRating)) =>
        (
          grossStats.add(holeScore.score, holeRating.par),
          netStats.add(holeScore.netScore, holeRating.par)
        )
      }

  lazy val ScoreStats(aces, eagles, birdies, pars, bogeys, others) = grossScoreStats
  lazy val ScoreStats(netAces, netEagles, netBirdies, netPars, netBogeys, netOthers) = netScoreStats

  private[this] case class ScoreStats(aces: Int, eagles: Int, birdies: Int,
    pars: Int, bogeys: Int, others: Int) {

    def add(score: Int, par: Int) = {

      val newAces = if(score == 1) aces + 1 else aces

      val parDiff = score - par

      parDiff match {
        case x if x <= -2 => copy(aces = newAces, eagles = eagles + 1)
        case x if x == -1 => copy(aces = newAces, birdies = birdies + 1)
        case x if x == 0 => copy(aces = newAces, pars = pars + 1)
        case x if x == 1 => copy(aces = newAces, bogeys = bogeys + 1)
        case _ => copy(aces = newAces, others = others + 1)
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
