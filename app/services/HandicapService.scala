package services

import com.github.nscala_time.time.Imports._

import scaldi.{ Injectable, Injector }

import models.{ Round, User }

trait HandicapService {

  def handicap(rounds: List[Round]): Option[Double]

  def handicap(round: Round, previous: List[Round]): Round

  def handicap(slope: Double, numHoles: Int, previous: List[Round]): Option[Double]
}

class DefaultHandicapService extends HandicapService {

  private[this] val MinRounds = 5
  private[this] val MaxRounds = 20
  private[this] val ExcellenceBonus = 0.96

  override def handicap(rounds: List[Round]): Option[Double] = {
    if(rounds.size >= MinRounds) {

      val validRounds = rounds.filter { r =>
        r.official && (r.numHoles == 9 || r.numHoles == 18)
      }

      val (halfRounds, fullRounds) = validRounds.partition(_.numHoles == 9)

      // Only pairs of half rounds can be counted
      val usableHalfRounds = halfRounds.take(MaxRounds).sorted.sliding(2,2).
        toList.collect { case List(a,b) => (a,b) }

      val usableFullRounds = fullRounds.take(MaxRounds)

      // Function will accumulate the most recent rounds by selecting the
      // the more recent of either a full round or a pair of 2 half rounds
      // together to create a full round
      @scala.annotation.tailrec
      def accumulateDifferentials(accum: List[Double],
        halfRoundPairs: List[(Round, Round)],
        fullRounds: List[Round]): List[Double] = {

        (halfRoundPairs,fullRounds) match {
          case _ if (accum.size >= MaxRounds) => accum
          case (Nil, Nil) => accum
          case (Nil, h :: t) => {
            accumulateDifferentials(h.handicapDifferential :: accum, Nil, t)
          }
          case ((a,b) :: t, Nil) => {
            val hcpDiff = (a.handicapDifferential + b.handicapDifferential)
            accumulateDifferentials(hcpDiff :: accum, t, Nil)
          }
          case ((a, b) :: ht, c :: ft) => {
            if(a.time > c.time && b.time > c.time) {
              val hcpDiff =
                (a.handicapDifferential + b.handicapDifferential)
              accumulateDifferentials(hcpDiff :: accum, ht, fullRounds)
            } else {
              accumulateDifferentials(
                c.handicapDifferential :: accum, halfRoundPairs, ft)
            }
          }
        }
      }

      val accumulatedDifferentials =
        accumulateDifferentials(Nil, usableHalfRounds, usableFullRounds)
      val differentialsToUse = accumulatedDifferentials.sorted.take(
        numRoundsToAverage(accumulatedDifferentials.size))

      if(differentialsToUse.nonEmpty) {
        val differential =
          differentialsToUse.sum / differentialsToUse.size * ExcellenceBonus
        val rounded = (differential * 10).round / 10d
        Some(rounded)
      } else {
        None
      }
    } else {
      None
    }
  }

  override def handicap(round: Round, previous: List[Round]): Round = {

    // First calculate the auto-handicap
    val slope =
      if(round.fullRound) {
        round.rating.slope
      } else if(round.front9) {
        round.rating.frontSlope
      } else {
        round.rating.backSlope
      }

    val withAutoHandicap =
      round.copy(handicap =
        handicap(slope, round.numHoles, previous).map(_.round.toInt))

    // If user wants to override it, then use their number
    val correctionOpt = round.handicapOverride orElse withAutoHandicap.handicap

    // Finally update all the holescores net scores
    correctionOpt.map { correction =>
      netCorrection(correction, withAutoHandicap)
    } getOrElse withAutoHandicap
  }

  override def handicap(slope: Double, numHoles: Int, previous: List[Round]): Option[Double] = {
    handicap(previous).map(hcp => (hcp / (18d / numHoles)) * slope / 113d)
  }

  private[this] def netCorrection(handicap: Int, round: Round): Round = {
    round.copy(
      holeScores = round.scoreRatings.map { case (score, rating) =>

        // If you're playing 9 holes of an 18 hole course, you need to divide
        // each holes true handicap by 2 to apply stokes to the appropriate
        // holes
        val ratingCorrection = round.course.numHoles / round.numHoles;

        val netCorrection =
          ((handicap - (rating.handicap.toDouble / ratingCorrection).ceil + 1) /
            round.numHoles).ceil.toInt.max(0)

        score.copy(netScore = score.score - netCorrection)
      }
    )
  }

  private[this] def numRoundsToAverage(numRounds: Int): Int = {
    numRounds match {
      case x if x < 5 => 0
      case 5  | 6     => 1
      case 7  | 8     => 2
      case 9  | 10    => 3
      case 11 | 12    => 4
      case 13 | 14    => 5
      case 15 | 16    => 6
      case 17         => 7
      case 18         => 8
      case 19         => 9
      case _          => 10
    }
  }
}