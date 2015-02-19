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
          DBHoleRating(hr.number, hr.par, hr.yardage, hr.handicap, ratingId)
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
      holeRatings.filter(_.ratingId === rating.id.getOrElse(-1L)).delete
      insert(rating)
    }
  }
}
