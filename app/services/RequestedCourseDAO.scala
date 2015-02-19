package services

import models.RequestedCourse

trait RequestedCourseDAO {

  def findById(id: Long): Option[RequestedCourse]

  def list(outstanding: Boolean, num: Int = RequestedCourseDAO.DefaultListSize,
    offset: Int = 0): List[RequestedCourse]

  def insert(request: RequestedCourse): RequestedCourse

  def forUser(usedId: Long, num: Int, offset: Int): List[RequestedCourse]

  def delete(id: Long): Int

  def update(id: Long, request: RequestedCourse): Option[RequestedCourse]
}

object RequestedCourseDAO {
  val DefaultListSize = 20
  val MaxListSize = 50
}
