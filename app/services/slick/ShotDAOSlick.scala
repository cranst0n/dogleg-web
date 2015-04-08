package services.slick

import scaldi.{ Injectable, Injector }

import play.api.Play.current
import play.api.db.slick._

import DoglegPostgresDriver.simple._
import Tables._

import models.{ HoleScore, LatLon, Shot }

import services.ShotDAO

class ShotDAOSlick(implicit val injector: Injector)
  extends ShotDAO with Injectable {

    override def insert(holeScore: HoleScore): List[Shot] = {
      DB withSession { implicit session =>

        val insertedShots =
          shots returning shots.map(_.id) into((shot, assignedId) =>
            shot.copy(id = Some(assignedId))
          ) ++= holeScore.shots.map(
            _.copy(holeScoreId = holeScore.id)).map(toDBShot)

        insertedShots.map(_.toShot).toList
      }
    }

    override def update(holeScore: HoleScore): List[Shot] = {
      DB withSession { implicit session =>
        holeScore.id.map { holeScoreId =>
          holeScore.shots.map { shot =>
            shots.filter(_.id === shot.id).
              map(s => (s.sequence, s.clubId, s.locationStart, s.locationEnd, s.holeScoreId)).
              update((shot.sequence, shot.club.id,
                LatLon.toVividPoint(shot.locationStart),
                LatLon.toVividPoint(shot.locationEnd), holeScoreId))

            shot
          }
        } getOrElse insert(holeScore)
      }
    }

    override def forHoleScore(holeScoreId: Long): List[Shot] = {
      DB withSession { implicit session =>
        shots.filter(_.holeScoreId === holeScoreId).list.map(_.toShot)
      }
    }

    private[this] def toDBShot(shot: Shot) = {
      DBShot(shot.id, shot.sequence, shot.club.id,
        LatLon.toVividPoint(shot.locationStart),
        LatLon.toVividPoint(shot.locationEnd), shot.holeScoreId)
    }
}
