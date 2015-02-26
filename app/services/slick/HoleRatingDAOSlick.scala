package services.slick

import scaldi.{ Injectable, Injector }

import play.api.Play.current
import play.api.db.slick._

import DoglegPostgresDriver.simple._
import Tables._

import models.{ CourseRating, HoleRating }

import services.HoleRatingDAO

class HoleRatingDAOSlick(implicit val injector: Injector)
  extends HoleRatingDAO with Injectable {

  override def insert(rating: CourseRating): List[HoleRating] = {
    rating.id.map { ratingId =>
      DB withSession { implicit session =>
        holeRatings ++= rating.holeRatings.map { hr =>
          DBHoleRating(hr.id, hr.number, hr.par, hr.yardage, hr.handicap, ratingId)
        }
        rating.holeRatings
      }
    } getOrElse Nil
  }

  override def forRating(ratingId: Long): List[HoleRating] = {
    DB withSession { implicit session =>
      holeRatings.filter(_.ratingId === ratingId).list.map(_.toHoleRating)
    }
  }

  override def update(rating: CourseRating): List[HoleRating] = {
    DB withSession { implicit session =>
      rating.holeRatings.map { holeRating =>
        holeRatings.filter(_.id === holeRating.id).
          map(hr => (hr.number, hr.par, hr.yardage, hr.handicap)).
          update((holeRating.number, holeRating.par,
            holeRating.yardage, holeRating.handicap))

          holeRating
      }
    }
  }
}
