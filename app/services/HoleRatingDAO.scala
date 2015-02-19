package services

import models.{ CourseRating, HoleRating }

trait HoleRatingDAO {

  def insert(rating: CourseRating): List[HoleRating]

  def forRating(ratingId: Long): List[HoleRating]

  def update(rating: CourseRating): List[HoleRating]
}
