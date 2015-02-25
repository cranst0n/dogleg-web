package services

import models.{ Course, Image, LatLon }

trait CourseDAO {

  def list(num: Int, offset: Int): List[Course] = {
    list(None, num, offset, true)
  }

  def list(num: Int, offset: Int, approved: Boolean): List[Course] = {
    list(None, num, offset, approved)
  }

  def list(location: Option[LatLon], num: Int, offset: Int,
    approved: Boolean): List[Course]

  def unapproved(num: Int, offset: Int): List[Course] = {
    list(None, num, offset, false)
  }

  def search(text: String, num: Int, offset: Int): List[Course]

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
