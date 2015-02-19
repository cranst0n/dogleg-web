package services.slick

import scaldi.{ Injectable, Injector }

import play.api.Play.current
import play.api.db.slick._

import DoglegPostgresDriver.simple._
import Tables._

import models.RequestedCourse

import services._

class RequestedCourseDAOSlick(implicit val injector: Injector)
  extends RequestedCourseDAO with Injectable {

  override def findById(id: Long): Option[RequestedCourse] = {
    DB withSession { implicit session =>
      requestedCourses.filter(_.id === id).firstOption.map(_.toRequestedCourse)
    }
  }

  override def list(outstanding: Boolean,
    num: Int = RequestedCourseDAO.DefaultListSize,
    offset: Int = 0): List[RequestedCourse] = {

    DB withSession { implicit session =>

      val filtered =
        if(outstanding) {
          requestedCourses.filter(_.fulfilledBy.isEmpty)
        } else {
          requestedCourses.filter(_.fulfilledBy.isDefined)
        }

      filtered.drop(offset).take(num).
        sortBy(_.id.asc).list.map(_.toRequestedCourse)
    }
  }

  override def insert(request: RequestedCourse): RequestedCourse = {
    DB withSession { implicit session =>
      requestedCourses returning requestedCourses.map(_.id) into ((dbRequest, assignedId) =>
        dbRequest.copy(id = assignedId).toRequestedCourse
      ) += DBRequestedCourse(request.id, request.name, request.city,
        request.state, request.country, request.website, request.comment,
        request.created, request.requestor.flatMap(_.id), None)
    }
  }

  override def forUser(userId: Long, num: Int, offset: Int): List[RequestedCourse] = {
    DB withSession { implicit session =>
      requestedCourses.filter(_.requestorId === userId).drop(offset).take(num).
        list.map(_.toRequestedCourse)
    }
  }

  override def delete(id: Long): Int = {
    DB withSession { implicit session =>
      requestedCourses.filter(_.id === id).delete
    }
  }

  override def update(id: Long, request: RequestedCourse): Option[RequestedCourse] = {
    DB withSession { implicit session =>
      requestedCourses.filter(_.id === id).
        map(r => (r.name,r.city,r.state,r.country,r.website,r.comment,r.fulfilledBy)).
        update((request.name, request.city, request.state, request.country,
          request.website, request.comment, request.fulfilledBy.flatMap(_.id)))

      findById(id)
    }
  }
}
