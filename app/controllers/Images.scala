package controllers

import scaldi.Injector

import com.sksamuel.scrimage.{ Image => Scrimage }
import com.sksamuel.scrimage.{ ImageTools => ScrimageTools }

import play.api.mvc._

import models.Image

import services.ImageDAO

class Images(implicit val injector: Injector) extends DoglegController with Security {

  lazy val imageDAO = inject[ImageDAO]

  def avatar(id: Long): Action[Unit] = Action(parse.empty) { implicit request =>
    imageDAO.findById(id).map { image =>
      Ok(
        ScrimageTools.toBytes(Scrimage(image.data).
          cover(Images.AvatarSize, Images.AvatarSize).toBufferedImage, "png")
      ).as("image/png")
    } getOrElse notFound("Unknown image ID")
  }

  def raw(id: Long): Action[Unit] = Action(parse.empty) { implicit request =>
    imageDAO.findById(id).map { image =>
      Ok(image.data).as("image/png")
    } getOrElse notFound("Unknown image ID")
  }
}

object Images {
  val AvatarSize = 30
}
