package services.slick

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

          val ratingId =
            courseRatings returning courseRatings.map(_.id) +=
              DBCourseRating(-1, rating.teeName, rating.rating, rating.slope,
                rating.frontRating, rating.frontSlope, rating.backRating,
                rating.backSlope, rating.bogeyRating,
                rating.gender.toString, courseId)

          val insertedHoleRatings =
            holeRatingDAO.insert(rating.copy(id = Option(ratingId)))

          rating.copy(id = Option(ratingId), holeRatings = insertedHoleRatings)

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
        course.ratings.map { rating =>

          // First remove any ratings not in the updated course
          val ratingIds = course.ratings.flatMap(_.id)
          courseRatings.filter(_.courseId === courseId).
            filter(rating => !(rating.id inSet ratingIds)).delete

          val updateHoleRatings = holeRatingDAO.update(rating)

          courseRatings.filter(_.id === rating.id).
            map(r => (r.teeName, r.rating, r.slope, r.frontRating, r.frontSlope,
              r.backRating, r.backSlope, r.bogeyRating, r.gender, r.courseId)).
            update((rating.teeName, rating.rating, rating.slope,
              rating.frontRating, rating.frontSlope, rating.backRating,
              rating.backSlope, rating.bogeyRating, rating.gender.toString,
              courseId))

          rating.copy(holeRatings = updateHoleRatings)
        }
      } getOrElse(Nil)
    }
  }
}
