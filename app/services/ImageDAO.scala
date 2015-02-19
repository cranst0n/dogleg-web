package services

import models.Image

trait ImageDAO {

  def findById(id: Long): Option[Image]

  def insertAll(images: List[Image]): List[Image] = images map insert

  def insert(image: Image): Image

  def update(image: Image): Option[Image]

  def delete(imageId: Long): Int
}
