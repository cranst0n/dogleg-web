package services

import models.{ Course, CourseRating }

trait CourseRatingDAO {

  def insert(course: Course): List[CourseRating]

  def findById(id: Long): Option[CourseRating]

  def forCourse(courseId: Long): List[CourseRating]

  def update(course: Course): List[CourseRating]
}
