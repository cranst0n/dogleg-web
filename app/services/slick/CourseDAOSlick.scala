package services.slick

import scala.slick.jdbc.{ StaticQuery => Q }

import scaldi.{ Injectable, Injector }

import play.api.Play.current
import play.api.db.slick._

import DoglegPostgresDriver.simple._
import Implicits._
import Tables._

import models.{ Course, Exclusivity, Image, LatLon }

import services._

class CourseDAOSlick(implicit val injector: Injector)
  extends CourseDAO with Injectable {

  lazy val courseRatingDAO = inject[CourseRatingDAO]
  lazy val holeDAO = inject[HoleDAO]
  lazy val userDAO = inject[UserDAO]
  lazy val imageDAO = inject[ImageDAO]

  override def list(location: Option[LatLon], num: Int, offset: Int,
    approved: Boolean): List[Course] = {

    DB withSession { implicit session =>

      val sortedRows =
        location.map { ll =>
          courses.sortBy(_.location <-> LatLon.toVividPoint(ll))
        }.getOrElse(courses.sortBy(_.id.asc))

      sortedRows.filter(_.approved === Option(approved)).
        drop(offset).take(num).list.map(_.toCourse)
    }
  }

  override def findById(id: Long): Option[Course] = {
    DB withSession { implicit session =>
      courses.filter(_.id === id).firstOption.map(_.toCourse)
    }
  }

  override def search(text: String, num: Int, offset: Int): List[Course] = {

    val sanitized =
      text.replaceAll("\\p{Punct}+", " ").replaceAll(" +", " ")

    DB withSession { implicit session =>
      Q.queryNA[DBCourse](s"""
        select * from course where approved = true and
          (
            (name || ' ' || city || ' ' || state || ' ' || country)
              ilike '%${sanitized}%' or
            (name || ' ' || state || ' ' || city || ' ' || country)
              ilike '%${sanitized}%'
          ) limit $num offset $offset
      """).list.map(_.toCourse)
    }
  }

  override def insert(course: Course): Course = {
    DB withSession { implicit session =>

      val courseId = courses returning courses.map(_.id) +=
        DBCourse(None, course.name, course.city, course.state, course.country,
          course.numHoles, course.exclusivity.toString, course.phoneNumber,
          course.location, course.creatorId, course.approved.getOrElse(false))

      val ratings = courseRatingDAO.insert(course.copy(id = Option(courseId)))
      val holes = holeDAO.insert(course.copy(id = Option(courseId)))

      course.copy(id = Option(courseId), ratings = ratings, holes = holes)
    }
  }

  override def delete(id: Long): Int = {
    DB withSession { implicit session =>
      courses.filter(_.id === id).delete
    }
  }

  override def approve(id: Long): Option[Course] = {
    DB withSession { implicit session =>
      courses.filter(_.id === id).map(_.approved).update(true)
      findById(id)
    }
  }

  override def update(course: Course): Option[Course] = {
    DB withSession { implicit session =>
      course.id.map { courseId =>

        courses.filter(_.id === courseId).
          map(c => (c.name, c.city, c.state, c.country, c.numHoles,
            c.exclusivity, c.phoneNumber, c.location, c.creatorId, c.approved)).
          update((course.name, course.city, course.state, course.country,
            course.numHoles, course.exclusivity.toString, course.phoneNumber,
            course.location, course.creatorId, course.approved.getOrElse(false)
          ))

        val updatedRating = courseRatingDAO.update(course)
        val updatedHoles = holeDAO.update(course)

        course.copy(ratings = updatedRating, holes = updatedHoles)
      }
    }
  }

  override def attachImage(courseId: Long, image: Image): Image = {
    DB withSession { implicit session =>
      val insertedImage = imageDAO.insert(image)
      courseImages += AttachedImage(courseId, insertedImage.id.getOrElse(-1))
      insertedImage
    }
  }

  override def detachImage(courseId: Long, imageId: Long): Int = {
    DB withSession { implicit session =>
      courseImages.
        filter(img => img.courseId === courseId && img.imageId === imageId).
        delete
    }
  }
}
