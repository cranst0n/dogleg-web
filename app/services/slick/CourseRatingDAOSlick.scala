package services.slick

import scala.slick.jdbc.JdbcBackend.SessionDef

import scaldi.{ Injectable, Injector }

import play.api.Play.current
import play.api.db.slick._

import DoglegPostgresDriver.simple._
import Tables._

import models.{ Course, CourseRating, Gender }
import Gender._

import services.{ CourseRatingDAO, HoleRatingDAO }

class CourseRatingDAOSlick(implicit val injector: Injector)
  extends CourseRatingDAO with Injectable {

  lazy val holeRatingDAO = inject[HoleRatingDAO]

  override def insert(course: Course): List[CourseRating] = {
    DB withSession { implicit session =>
      course.id.map { courseId =>
        course.ratings.map { rating =>
          insert(courseId, rating)
        }
      } getOrElse Nil
    }
  }

  override def findById(id: Long): Option[CourseRating] = {
    DB withSession { implicit session =>
      courseRatings.filter(_.id === id).firstOption.map(_.toCourseRating)
    }
  }

  override def forCourse(courseId: Long): List[CourseRating] = {
    DB withSession { implicit session =>
      courseRatings.filter(_.courseId === courseId).list.
        map(_.toCourseRating).sorted
    }
  }

  override def update(course: Course): List[CourseRating] = {
    DB withSession { implicit session =>
      course.id map { courseId =>

        val updatedRatingIds = course.ratings.flatMap(_.id)

        course.ratings.map { rating =>

          rating.id.map { idToUpdate =>

            // First remove any ratings not in the updated course
            courseRatings.filter(_.courseId === courseId).
              filter(rating => !(rating.id inSet updatedRatingIds)).delete

            update(idToUpdate, courseId, rating)

          } getOrElse insert(courseId, rating)
        }
      } getOrElse(Nil)
    }
  }

  private[this] def insert(courseId: Long, rating: CourseRating)(implicit sessionDef: SessionDef) = {

    val ratingId =
      courseRatings returning courseRatings.map(_.id) +=
        DBCourseRating(None, rating.teeName, rating.rating, rating.slope,
          rating.frontRating, rating.frontSlope, rating.backRating,
          rating.backSlope, rating.bogeyRating,
          rating.gender.toString, courseId)

    val insertedHoleRatings =
      holeRatingDAO.insert(rating.copy(id = Option(ratingId)))

    rating.copy(id = Option(ratingId), holeRatings = insertedHoleRatings)
  }

  private[this] def update(id: Long, courseId: Long, rating: CourseRating)(implicit sessionDef: SessionDef) = {

    val updateHoleRatings = holeRatingDAO.update(rating)

    courseRatings.filter(_.id === id).
      map(r => (r.teeName, r.rating, r.slope, r.frontRating, r.frontSlope,
        r.backRating, r.backSlope, r.bogeyRating, r.gender, r.courseId)).
      update((rating.teeName, rating.rating, rating.slope,
        rating.frontRating, rating.frontSlope, rating.backRating,
        rating.backSlope, rating.bogeyRating, rating.gender.toString,
        courseId))

    rating.copy(holeRatings = updateHoleRatings)
  }
}
