package services.slick

import scaldi.{ Injectable, Injector }

import play.api.Play.current
import play.api.db.slick._

import DoglegPostgresDriver.simple._
import Tables._

import models.{ HoleScore, Round }

import services.{ HoleDAO, HoleScoreDAO }

class HoleScoreDAOSlick(implicit val injector: Injector)
  extends HoleScoreDAO with Injectable {

  val holeDAO = inject[HoleDAO]

  override def insert(round: Round): List[HoleScore] = {
    DB withSession { implicit session =>
      holeScores ++= round.holeScores.map { s =>
        toDBScore(s).copy(roundId = round.id)
      }
      round.holeScores
    }
  }

  override def update(round: Round): List[HoleScore] = {
    DB withSession { implicit session =>
      holeScores.filter(_.roundId === round.id).delete
      insert(round)
    }
  }

  override def forRound(roundId: Long): List[HoleScore] = {
    DB withSession { implicit session =>
      holeScores.filter(_.roundId === roundId).list.map(_.toHoleScore)
    }
  }

  private[this] def toDBScore(hs: HoleScore): DBHoleScore = {
    DBHoleScore(hs.score, hs.netScore, hs.putts, hs.penaltyStrokes,
      hs.fairwayHit, hs.gir, hs.roundId, hs.hole.id.getOrElse(-1))
  }
}
