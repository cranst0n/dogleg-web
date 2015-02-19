package services

import models.{ Course, Hole }

trait HoleDAO {

  def forCourse(courseId: Long): List[Hole]

  def findById(id: Long): Option[Hole]

  def insert(course: Course): List[Hole]

  def update(course: Course): List[Hole]
}
