package controllers

import scaldi.Injector

import com.sksamuel.scrimage.{ Image => Scrimage }
import com.sksamuel.scrimage.{ ImageTools => ScrimageTools }

import play.api.Play
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.mvc._

import models.{ CourseSummary, Image, User }

import services.{ CourseDAO, ImageDAO }

case class ChangePasswordRequest(oldPassword: String, newPassword: String,
  newPasswordConfirm: String)

object ChangePasswordRequest {
  implicit val JsonFormat = Json.format[ChangePasswordRequest]
}

case class UpdateProfileRequest(home: Option[String], favoriteCourse: Option[CourseSummary])

object UpdateProfileRequest {
  implicit val JsonFormat = Json.format[UpdateProfileRequest]
}

class Users(implicit val injector: Injector) extends DoglegController with Security {

  lazy val imageDAO = inject[ImageDAO]
  lazy val courseDAO = inject[CourseDAO]

  private[this] val signupEnabled =
    Play.current.configuration.getBoolean("signup.enabled").getOrElse(false)

  def user(id: Long): Action[Unit] = HasToken(parse.empty) { implicit request =>
    userDAO.findById(id).map(u => Ok(Json.toJson(u))).
      getOrElse(notFound("Unknown user"))
  }

  def createUser(): Action[JsValue] = Action(parse.json) { implicit request =>
    expect[User] { user =>
      if(signupEnabled) {
        userDAO.findByName(user.name) match {
          case Some(_) => badRequest(s"'${user.name}' is already taken.")
          case None => Ok(Json.toJson(userDAO.insert(user)))
        }
      } else {
        notImplemented("Sign up not allowed at this time.")
      }
    }
  }

  def updateUser(id: Long): Action[JsValue] = HasToken(parse.json) { implicit request =>
    expect[User] { newUserInfo =>
      sameUserOrAdmin(id) {
        userDAO.update(id,newUserInfo).map { updatedUser =>
          Ok(Json.toJson(updatedUser))
        } getOrElse notFound("Unknown user")
      }("A user may only update their own information")
    }
  }

  def updateProfile(id: Long): Action[JsValue] = HasToken(parse.json) { implicit request =>
    expect[UpdateProfileRequest] { newUserInfo =>
      sameUserOrAdmin(id) {

        val favoriteCourse =
          for {
            summary <- newUserInfo.favoriteCourse
            courseId <- summary.id
            course <- courseDAO.findById(courseId)
          } yield {
            course
          }

        val newProfile = request.user.profile.copy(
          home = newUserInfo.home,
          favoriteCourse = favoriteCourse
        )

        userDAO.update(id, request.user.copy(profile = newProfile)).map { updatedUser =>
          Ok(Json.toJson(updatedUser))
        } getOrElse notFound("Unknown user")
      }("A user may only update their own information")
    }
  }

  def changePassword(id: Long): Action[JsValue] = HasToken(parse.json) { implicit request =>
    expect[ChangePasswordRequest] { changeRequest =>
      sameUserOrAdmin(id) {

        // First, check that old password is valid
        userDAO.authenticate(request.user.name, changeRequest.oldPassword).map { authenticated =>
          if(changeRequest.newPassword == changeRequest.newPasswordConfirm) {
            userDAO.findById(id).map { user =>
              Ok(Json.toJson(
                userDAO.changePassword(user, changeRequest.newPassword)
              ))
            } getOrElse notFound("Unknown user")
          } else {
            badRequest("Passwords do not match")
          }
        } getOrElse badRequest("Old password incorrect")
      }("A user may only change their own password")
    }
  }

  def avatar(id: Long, width: Option[Int], height: Option[Int]): Action[Unit] = Action(parse.empty) { implicit request =>
    val image =
      userDAO.findById(id).flatMap { user =>
        user.profile.avatar
      } getOrElse Images.DefaultAvatar

      Ok(
        ScrimageTools.toBytes(Scrimage(image.data).
          cover(width.getOrElse(Images.AvatarSize),
            width.getOrElse(Images.AvatarSize)).toBufferedImage, "png")
      ).as("image/png")
  }

  def changeAvatar(id: Long): Action[JsValue] = HasToken(parse.json) { implicit request =>
    expect[Option[FileUpload]] { file =>
      sameUserOrAdmin(id) {

        val image = file.map(file => Image(None, file.bytes))

        Ok(Json.toJson(userDAO.setAvatar(request.user, image)))

      }("A user may only change their own avatar")
    }
  }

  def deleteUser(id: Long): Action[Unit] = HasToken(parse.empty) { implicit request =>
    sameUserOrAdmin(id) {
      if(userDAO.delete(id) == 1) {
        ok("User deleted")
      } else {
        notFound("Unknown user")
      }
    }("A user may only delete their own account")
  }

  private[this] def sameUserOrAdmin[A](id: Long)(validBlock: => Result)(forbiddenMessage: String)(implicit request: TokenRequest[A]) = {
    val requestorId = request.user.id.getOrElse(-1L)
    if(requestorId == id || request.user.admin) {
      validBlock
    } else {
      forbidden(forbiddenMessage)
    }
  }
}
