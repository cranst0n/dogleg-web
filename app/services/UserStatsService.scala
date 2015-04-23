package services

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

import scaldi.{ Injectable, Injector }

import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._

import models.{ User, UserStats }

trait UserStatsService {

  def forUser(user: User): Future[UserStats]

  def update(user: User): Future[UserStats]
}

class DefaultUserStatsService(implicit val injector: Injector)
  extends UserStatsService with Injectable {

  lazy val roundDAO = inject[RoundDAO]

  override def forUser(user: User): Future[UserStats] = {
    Cache.getAs[String](cacheKey(user)).map { statsJson =>
      Try {
        Json.parse(statsJson).validate[UserStats] match {
          case JsSuccess(stats, _) => Future.successful(stats)
          case JsError(_) => update(user)
        }
      } match {
        case Success(v) => v
        case Failure(_) => update(user)
      }
    } getOrElse update(user)
  }

  override def update(user: User): Future[UserStats] = {
    Future {

      // get ALLLLL the rounds.
      val userRounds = roundDAO.list(user, Int.MaxValue, 0).sorted

      val frequentCourses = userRounds.map(_.course.summary).
        groupBy(_.id).values.toList.
        sortWith(_.size > _.size).
        map(_.headOption).flatten.take(3)

      val autoHandicap = userRounds.headOption.flatMap(_.handicap).getOrElse(0)

      val rounds9Holes = userRounds.filter(_.numHoles == 9)
      val rounds18Holes = userRounds.filter(_.numHoles == 18)

      val lowGross9Hole =
        if(rounds9Holes.nonEmpty) rounds9Holes.map(_.score).min
        else 0
      val lowGross18Hole =
        if(rounds18Holes.nonEmpty) rounds18Holes.map(_.score).min
        else 0
      val averageGross18Hole =
        if(rounds18Holes.nonEmpty)
          rounds18Holes.map(_.score).sum / rounds18Holes.size.toDouble
        else 0

      val lowNet9Hole =
        if(rounds9Holes.nonEmpty) rounds9Holes.map(_.netScore).min
        else 0
      val lowNet18Hole =
        if(rounds18Holes.nonEmpty) rounds18Holes.map(_.netScore).min
        else 0
      val averageNet18Hole =
        if(rounds18Holes.nonEmpty)
          rounds18Holes.map(_.netScore).sum / rounds18Holes.size.toDouble
        else 0

      val scoreRatings = userRounds.flatMap(_.scoreRatings)

      val fairwayHitPercentage = {
        val (fairwayHitOpportunities, fairwaysHit) =
          scoreRatings.foldLeft(0,0) { case ((opps, hits), (score, rating)) =>
            rating.par match {
              case p if p > 3 && score.fairwayHit => (opps + 1, hits + 1)
              case p if p > 3 => (opps + 1, hits)
              case _ => (opps, hits)
            }
          }

        fairwaysHit / fairwayHitOpportunities.toDouble
      }

      val girPercentage =
        scoreRatings.count { case (score, rating) => score.gir } / scoreRatings.size.toDouble

      val grossBirdieStreak =
        longestStreak(scoreRatings) { case (score, rating) =>
          score.score < rating.par
        }

      val netBirdieStreak =
        longestStreak(scoreRatings) { case (score, rating) =>
          score.netScore < rating.par
        }

      val grossParStreak =
        longestStreak(scoreRatings) { case (score, rating) =>
          score.score <= rating.par
        }

      val netParStreak =
        longestStreak(scoreRatings) { case (score, rating) =>
          score.netScore <= rating.par
        }

      val fewestPutts18Hole =
        if(rounds18Holes.nonEmpty) rounds18Holes.map(_.putts).min
        else 0

      val grossMostBirdies18Hole =
        if(rounds18Holes.nonEmpty) rounds18Holes.map(_.birdies).max
        else 0
      val grossMostPars18Hole =
        if(rounds18Holes.nonEmpty) rounds18Holes.map(_.pars).max
        else 0

      val netMostBirdies18Hole =
        if(rounds18Holes.nonEmpty) rounds18Holes.map(_.netBirdies).max
        else 0
      val netMostPars18Hole =
        if(rounds18Holes.nonEmpty) rounds18Holes.map(_.netPars).max
        else 0

      val (totalHoles, totalPutts, totalPenalties,
        grossAces, grossEagles, grossBirdies, grossPars,
        netAces, netEagles, netBirdies, netPars) =
          userRounds.foldLeft((0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
            { case ((holes, putts, penalties,
              grossAces, grossEagles, grossBirdies, grossPars,
              netAces, netEagles, netBirdies, netPars), round) =>

              (holes + round.numHoles,
                putts + round.putts,
                penalties + round.penaltyStrokes,
                grossAces + round.aces,
                grossEagles + round.eagles,
                grossBirdies + round.birdies,
                grossPars + round.pars,
                netAces + round.netAces,
                netEagles + round.netEagles,
                netBirdies + round.netBirdies,
                netPars + round.netPars
              )
            }

      val averagePuttPerHole =
        if(totalHoles > 0) totalPutts / totalHoles.toDouble
        else 0
      val averagePenaltiesPerRound =
        if(userRounds.nonEmpty) totalPenalties / userRounds.size.toDouble
        else 0

      val grossAverageEaglesPerRound =
        if(userRounds.nonEmpty) grossEagles / userRounds.size.toDouble
        else 0
      val grossAverageBirdiesPerRound =
        if(userRounds.nonEmpty) grossBirdies / userRounds.size.toDouble
        else 0
      val grossAverageParsPerRound =
        if(userRounds.nonEmpty) grossPars / userRounds.size.toDouble
        else 0

      val netAverageEaglesPerRound =
        if(userRounds.nonEmpty) netEagles / userRounds.size.toDouble
        else 0
      val netAverageBirdiesPerRound =
        if(userRounds.nonEmpty) netBirdies / userRounds.size.toDouble
        else 0
      val netAverageParsPerRound =
        if(userRounds.nonEmpty) netPars / userRounds.size.toDouble
        else 0

      val (numPar3s, par3GrossSum, par3NetSum, numPar4s, par4GrossSum, par4NetSum, numPar5s, par5GrossSum, par5NetSum) =
        userRounds.flatMap(_.scoreRatings).foldLeft((0, 0, 0, 0, 0, 0, 0, 0, 0))
          { case ((np3, p3gs, p3ns, np4, p4gs, p4ns, np5, p5gs, p5ns), (holeScore, holeRating)) =>

            holeRating.par match {
              case 3 => {
                (np3 + 1, p3gs + holeScore.score, p3ns + holeScore.netScore, np4, p4gs, p4ns, np5, p5gs, p5ns)
              }
              case 4 => {
                (np3, p3gs, p3ns, np4 + 1, p4gs + holeScore.score, p4ns + holeScore.netScore, np5, p5gs, p5ns)
              }
              case 5 => {
                (np3, p3gs, p3ns, np4, p4gs, p4ns, np5 + 1, p5gs + holeScore.score, p5ns + holeScore.netScore)
              }
              case _ => (np3, p3gs, p3ns, np4, p4gs, p4ns, np5, p5gs, p5ns)
            }
          }

      val grossPar3Average =
        if(numPar3s > 0) par3GrossSum / numPar3s.toDouble
        else 0
      val grossPar4Average =
        if(numPar4s > 0) par4GrossSum / numPar4s.toDouble
        else 0
      val grossPar5Average =
        if(numPar5s > 0) par5GrossSum / numPar5s.toDouble
        else 0

      val netPar3Average =
        if(numPar3s > 0) par3NetSum / numPar3s.toDouble
        else 0
      val netPar4Average =
        if(numPar4s > 0) par4NetSum / numPar4s.toDouble
        else 0
      val netPar5Average =
        if(numPar5s > 0) par5NetSum / numPar5s.toDouble
        else 0

      val stats =
        UserStats(user, frequentCourses,
          autoHandicap, userRounds.size,
          lowGross9Hole, lowGross18Hole, averageGross18Hole,
          lowNet9Hole, lowNet18Hole, averageNet18Hole,
          fairwayHitPercentage, girPercentage,
          grossAces, grossBirdieStreak, grossParStreak,
          netAces, netBirdieStreak, netParStreak,
          fewestPutts18Hole,
          grossMostBirdies18Hole, grossMostPars18Hole,
          netMostBirdies18Hole, netMostPars18Hole,
          averagePuttPerHole, averagePenaltiesPerRound,
          grossAverageEaglesPerRound, grossEagles,
          grossAverageBirdiesPerRound, grossBirdies,
          grossAverageParsPerRound, grossPars,
          netAverageEaglesPerRound, netEagles,
          netAverageBirdiesPerRound, netBirdies,
          netAverageParsPerRound, netPars,
          grossPar3Average, grossPar4Average, grossPar5Average,
          netPar3Average, netPar4Average, netPar5Average
        )

      Cache.set(cacheKey(user), Json.toJson(stats).toString)

      stats
    }
  }

  private[this] def longestStreak[A](l: List[A])(predicate: A => Boolean): Int = {
    l match {
      case Nil => 0
      case head :: tail => {
        l.prefixLength(predicate).max(longestStreak(tail)(predicate))
      }
    }
  }

  private[this] def cacheKey(user: User) = {
    s"user.stats.${user.id.getOrElse(-1)}"
  }
}
