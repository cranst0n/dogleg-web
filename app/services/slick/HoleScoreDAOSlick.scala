package services.slick

import scaldi.{ Injectable, Injector }

import play.api.Play.current
import play.api.db.slick._

import DoglegPostgresDriver.simple._
import Tables._

import models.{ HoleScore, Round }

import services.{ HoleDAO, HoleScoreDAO, ShotDAO }

class HoleScoreDAOSlick(implicit val injector: Injector)
  extends HoleScoreDAO with Injectable {

  val holeDAO = inject[HoleDAO]
  val shotDAO = inject[ShotDAO]

  override def insert(round: Round): List[HoleScore] = {
    DB withSession { implicit session =>

      round.holeScores.map { holeScore =>

        val insertedDBHoleScore =
          holeScores returning holeScores.map(_.id) into((hs, assignedId) =>
            hs.copy(id = Some(assignedId))
          ) += toDBScore(holeScore.copy(roundId = round.id))

        insertedDBHoleScore.toHoleScore.copy(
          shots = shotDAO.insert(holeScore.copy(id = insertedDBHoleScore.id))
        )
      }
    }
  }

  override def update(round: Round): List[HoleScore] = {
    DB withSession { implicit session =>
      round.holeScores.map { holeScore =>

        holeScores.filter(_.id === holeScore.id).
          map(hs => (hs.score, hs.netScore, hs.putts, hs.penaltyStrokes,
            hs.fairwayHit, hs.gir, hs.roundId, hs.holeId)).
          update((holeScore.score, holeScore.netScore, holeScore.putts,
            holeScore.penaltyStrokes, holeScore.fairwayHit, holeScore.gir,
            round.id, holeScore.hole.id.getOrElse(-1)))

        holeScore.copy(
          shots = shotDAO.update(holeScore)
        )
      }
    }
  }

  override def forRound(roundId: Long): List[HoleScore] = {
    DB withSession { implicit session =>
      holeScores.filter(_.roundId === roundId).list.map(_.toHoleScore)
    }
  }

  private[this] def toDBScore(hs: HoleScore): DBHoleScore = {
    DBHoleScore(hs.id, hs.score, hs.netScore, hs.putts, hs.penaltyStrokes,
      hs.fairwayHit, hs.gir, hs.roundId, hs.hole.id.getOrElse(-1))
  }
}
