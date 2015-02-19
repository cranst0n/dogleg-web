package services.slick

import scaldi.{ Injectable, Injector }

import play.api.Play.current
import play.api.db.slick._

import DoglegPostgresDriver.simple._
import Tables._

import models.{ Course, Hole }

import services.{ HoleDAO, HoleFeatureDAO }

class HoleDAOSlick(implicit val injector: Injector)
  extends HoleDAO with Injectable {

  lazy val holeFeatureDAO = inject[HoleFeatureDAO]

  override def forCourse(courseId: Long): List[Hole] = {
    DB withSession { implicit session =>
      holes.filter(_.courseId === courseId).list.map(_.toHole)
    }
  }

  override def findById(id: Long): Option[Hole] = {
    DB withSession { implicit session =>
      holes.filter(_.id === id).firstOption.map(_.toHole)
    }
  }

  override def insert(course: Course): List[Hole] = {
    DB withSession { implicit session =>
      course.holes.flatMap { hole =>
        course.id.map { courseId =>
          val holeId = holes returning holes.map(_.id) +=
            DBHole(hole.id.getOrElse(-1), hole.number, courseId)

          hole.copy(
            id = Some(holeId),
            features = holeFeatureDAO.insert(hole.copy(id = Some(holeId))),
            courseId = course.id
          )
        }
      }
    }
  }

  override def update(course: Course): List[Hole] = {
    DB withSession { implicit session =>

      // First remove any holes not in the updated course
      course.id map { courseId =>
        val holeIds = course.holes.flatMap(_.id)
        holes.filter(_.courseId === courseId).
          filter(hole => !(hole.id inSet holeIds)).delete
      }

      for {
        hole <- course.holes
        holeId <- hole.id
        courseId <- hole.courseId
      } yield {
        val updatedFeatures = holeFeatureDAO.update(hole)

        holes.filter(_.id === holeId).
          map(h => (h.number, h.courseId)).
          update((hole.number,courseId))

        hole.copy(features = updatedFeatures)
      }
    }
  }
}
