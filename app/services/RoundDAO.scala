package services

import org.joda.time.DateTime

import models.{ Image, Round, User }

trait RoundDAO {

  def insert(round: Round): Round

  def update(round: Round): Option[Round]

  def delete(id: Long): Option[Round]

  def list(user: User, num: Int, offset: Int): List[Round]

  def before(user: User, time: DateTime): List[Round]

  def after(user: User, time: DateTime): List[Round]

  def findById(id: Long): Option[Round]

  def attachImage(roundId: Long, image: Image): Image

  def detachImage(roundId: Long, imageId: Long): Int
}

object RoundDAO {
  val DefaultListSize = 20
  val MaxListSize = 50
}
