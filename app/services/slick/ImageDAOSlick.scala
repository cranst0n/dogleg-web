package services.slick

import scaldi.{ Injectable, Injector }

import play.api.Play.current
import play.api.db.slick._

import DoglegPostgresDriver.simple._
import Tables._

import models.Image

import services.ImageDAO

class ImageDAOSlick(implicit val injector: Injector)
  extends ImageDAO with Injectable {

  override def findById(id: Long): Option[Image] = {
    DB withSession { implicit session =>
      images.filter(_.id === id).firstOption
    }
  }

  override def insert(image: Image): Image = {
    DB withSession { implicit session =>
      images returning images.map(_.id) into ((image, assignedId) =>
        image.copy(id = Some(assignedId))
      ) += image
    }
  }

  override def update(image: Image): Option[Image] = {
    image.id.map { existingId =>
      DB withSession { implicit session =>
        images.filter(_.id === existingId).
          map(i => (i.data)).
          update(image.data)

        image
      }
    }
  }

  override def delete(imageId: Long): Int = {
    DB withSession { implicit session =>
      images.filter(_.id === imageId).delete
    }
  }
}
