package controllers

import scaldi.Injector

import org.apache.commons.io.IOUtils

import com.sksamuel.scrimage.{ Image => Scrimage }
import com.sksamuel.scrimage.{ ImageTools => ScrimageTools }

import play.api.Play
import play.api.Play.current
import play.api.mvc._

import models.Image

import services.ImageDAO

import utils.FileUtils

class Images(implicit val injector: Injector) extends DoglegController with Security {

  lazy val imageDAO = inject[ImageDAO]

  def avatar(id: Long, width: Option[Int], height: Option[Int]): Action[Unit] = Action(parse.empty) { implicit request =>
    imageDAO.findById(id).map { image =>
      Ok(
        ScrimageTools.toBytes(Scrimage(image.data).
          cover(width.getOrElse(Images.AvatarSize),
            width.getOrElse(Images.AvatarSize)).toBufferedImage, "png")
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
  val DefaultAvatar = {
    Play.resourceAsStream("public/images/default_avatar.png") map { stream =>
      Image(None, IOUtils.toByteArray(stream))
    } getOrElse Image(None, Array())
  }
}
