package services

import models.{ Course, Image }

trait CourseDAO {

  def list(num: Int = CourseDAO.DefaultListSize, offset: Int = 0,
    approved: Boolean = true): List[Course]

  def unapproved(num: Int = CourseDAO.DefaultListSize, offset: Int = 0): List[Course] = {
    list(num, offset, false)
  }

  def search(text: String, num: Int = CourseDAO.DefaultListSize,
    offset: Int = 0): List[Course]

  def findById(id: Long): Option[Course]

  def insert(course: Course): Course

  def delete(id: Long): Int

  def approve(id: Long): Option[Course]

  def update(course: Course): Option[Course]

  def attachImage(courseId: Long, image: Image): Image

  def detachImage(courseId: Long, imageId: Long): Int
}

object CourseDAO {
  val DefaultListSize = 20
  val MaxListSize = 50
}
