package services.slick

import scaldi.{ Injectable, Injector }

import play.api.Play.current
import play.api.db.slick._

import DoglegPostgresDriver.simple._
import Tables._

import models.{ Image, Round, User }

import services._

class RoundDAOSlick(implicit val injector: Injector)
  extends RoundDAO with Injectable {

  lazy val userDAO = inject[UserDAO]
  lazy val courseDAO = inject[CourseDAO]
  lazy val courseRatingDAO = inject[CourseRatingDAO]
  lazy val holeScoreDAO = inject[HoleScoreDAO]
  lazy val imageDAO = inject[ImageDAO]

  override def insert(round: Round): Round = {
    DB withSession { implicit session =>

      val dbRound = DBRound(round.id, round.time, round.official,
        round.user.id.getOrElse(-1L), round.course.id.getOrElse(-1L),
        round.rating.id.getOrElse(-1L), round.handicap, round.handicapOverride)

      val insertedRound: Round =
        rounds returning rounds.map(_.id) into ((r, id) =>
          round.copy(id = id)
        ) += dbRound

      holeScoreDAO.insert(insertedRound)
      insertedRound
    }
  }

  override def update(round: Round): Option[Round] = {
    DB withSession { implicit session =>

      for {
        courseId <- round.course.id
        ratingId <- round.rating.id
      } yield {

        rounds.filter(_.id === round.id).
          map(r => (r.courseId, r.time, r.official, r.ratingId, r.handicap, r.handicapOverride)).
          update((courseId, round.time, round.official, ratingId, round.handicap, round.handicapOverride))

        holeScoreDAO.update(round)
        round
      }
    }
  }

  override def delete(id: Long): Int = {
    DB withSession { implicit session =>
      rounds.filter(_.id === id).delete
    }
  }

  override def list(user: User, num: Int = RoundDAO.DefaultListSize,
    offset: Int = 0): List[Round] = {

    DB withSession { implicit session =>
      user.id.map { userId =>
        rounds.
          filter(_.userId === userId).
          drop(offset).
          take(num).
          sortBy(_.time.desc).
          list.
          map(_.toRound)
      } getOrElse Nil
    }
  }

  override def findById(id: Long): Option[Round] = {
    DB withSession { implicit session =>
      rounds.filter(_.id === id).firstOption.map(_.toRound)
    }
  }

  override def attachImage(roundId: Long, image: Image): Image = {
    DB withSession { implicit session =>
      val insertedImage = imageDAO.insert(image)
      roundImages += AttachedImage(roundId, insertedImage.id.getOrElse(-1))
      insertedImage
    }
  }

  override def detachImage(roundId: Long, imageId: Long): Int = {
    DB withSession { implicit session =>
      roundImages.
        filter(img => img.roundId === roundId && img.imageId === imageId).
        delete
    }
  }
}
