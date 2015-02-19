package services

import models.{ Image, Round, User }

trait RoundDAO {

  def insert(round: Round): Round

  def update(round: Round): Option[Round]

  def delete(id: Long): Int

  def list(user: User, num: Int = RoundDAO.DefaultListSize,
    offset: Int = 0): List[Round]

  def findById(id: Long): Option[Round]

  def attachImage(roundId: Long, image: Image): Image

  def detachImage(roundId: Long, imageId: Long): Int
}

object RoundDAO {
  val DefaultListSize = 20
  val MaxListSize = 50
}
