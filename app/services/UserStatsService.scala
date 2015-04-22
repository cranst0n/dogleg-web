package services

import scala.concurrent.Future

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
      Json.parse(statsJson).validate[UserStats] match {
        case JsSuccess(stats, _) => Future.successful(stats)
        case JsError(_) => update(user)
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
          rounds18Holes.map(_.score).sum / rounds18Holes.size
        else 0

      val lowNet9Hole =
        if(rounds9Holes.nonEmpty) rounds9Holes.map(_.netScore).min
        else 0
      val lowNet18Hole =
        if(rounds18Holes.nonEmpty) rounds18Holes.map(_.netScore).min
        else 0
      val averageNet18Hole =
        if(rounds18Holes.nonEmpty)
          rounds18Holes.map(_.netScore).sum / rounds18Holes.size
        else 0

      val scoreRatings = userRounds.flatMap(_.scoreRatings)
      val birdieStreak =
        longestStreak(scoreRatings){ case (score, rating) =>
          score.score < rating.par
        }

      val parStreak =
        longestStreak(scoreRatings){ case (score, rating) =>
          score.score <= rating.par
        }

      val lowPutts18Hole =
        if(rounds18Holes.nonEmpty) rounds18Holes.map(_.putts).min
        else 0
      val mostBirdies18Hole =
        if(rounds18Holes.nonEmpty) rounds18Holes.map(_.birdies).max
        else 0
      val mostPars18Hole =
        if(rounds18Holes.nonEmpty) rounds18Holes.map(_.pars).max
        else 0

      val (totalHoles, totalPutts, totalAces, totalEagles, totalBirdies, totalPars) =
        userRounds.foldLeft((0, 0, 0, 0, 0, 0))
          { case ((holes, putts, aces, eagles, birdies, pars), round) =>
            (holes + round.numHoles,
              putts + round.putts,
              aces + round.aces,
              eagles + round.eagles,
              birdies + round.birdies,
              pars + round.pars
            )
          }

      val averagePuttPerHole =
        if(totalHoles > 0) totalPutts / totalHoles.toDouble
        else 0

      val averageEaglesPerRound =
        if(userRounds.nonEmpty) totalEagles / userRounds.size.toDouble
        else 0
      val averageBirdiesPerRound =
        if(userRounds.nonEmpty) totalBirdies / userRounds.size.toDouble
        else 0
      val averageParsPerRound =
        if(userRounds.nonEmpty) totalPars / userRounds.size.toDouble
        else 0

      val (numPar3s, par3Sum, numPar4s, par4Sum, numPar5s, par5Sum) =
        userRounds.flatMap(_.scoreRatings).foldLeft((0, 0, 0, 0, 0, 0))
          { case ((np3, p3s, np4, p4s, np5, p5s), (holeScore, holeRating)) =>
            holeRating.par match {
              case 3 => (np3 + 1, p3s + holeScore.score, np4, p4s, np5, p5s)
              case 4 => (np3, p3s, np4 + 1, p4s + holeScore.score, np5, p5s)
              case 5 => (np3, p3s, np4, p4s, np5 + 1, p5s + holeScore.score)
              case _ => (np3, p3s, np4, p4s, np5, p5s)
            }
          }

      val par3Average =
        if(numPar3s > 0) par3Sum / numPar3s.toDouble
        else 0
      val par4Average =
        if(numPar4s > 0) par4Sum / numPar4s.toDouble
        else 0
      val par5Average =
        if(numPar5s > 0) par5Sum / numPar5s.toDouble
        else 0

      val stats =
        UserStats(user, frequentCourses,
          autoHandicap, userRounds.size,
          lowGross9Hole, lowGross18Hole, averageGross18Hole,
          lowNet9Hole, lowNet18Hole, averageNet18Hole,
          totalAces, birdieStreak, parStreak,
          lowPutts18Hole, mostBirdies18Hole, mostPars18Hole,
          averagePuttPerHole,
          averageEaglesPerRound, totalEagles,
          averageBirdiesPerRound, totalBirdies,
          averageParsPerRound, totalPars,
          par3Average, par4Average, par5Average
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
