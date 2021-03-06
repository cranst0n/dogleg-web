package controllers

import scala.concurrent.Future

import scaldi.Injector

import com.sksamuel.scrimage.{ Image => Scrimage }
import com.sksamuel.scrimage.{ ImageTools => ScrimageTools }

import play.api.Play
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.mvc._

import models.{ CourseSummary, Image, User }

import services.{ CourseDAO, ImageDAO, MailerService, UserStatsService }

case class ChangePasswordRequest(oldPassword: String, newPassword: String,
  newPasswordConfirm: String)

object ChangePasswordRequest {
  implicit val JsonFormat = Json.format[ChangePasswordRequest]
}

case class ResetPasswordRequest(newPassword: String, newPasswordConfirm: String)

object ResetPasswordRequest {
  implicit val JsonFormat = Json.format[ResetPasswordRequest]
}

case class UpdateProfileRequest(home: Option[String], favoriteCourse: Option[CourseSummary])

object UpdateProfileRequest {
  implicit val JsonFormat = Json.format[UpdateProfileRequest]
}

class Users(implicit val injector: Injector) extends DoglegController with Security {

  private[this] val avatarMaxSize = 10 * 1024 * 1024

  lazy val mailer = inject[MailerService]
  lazy val userStatsService = inject[UserStatsService]
  lazy val imageDAO = inject[ImageDAO]
  lazy val courseDAO = inject[CourseDAO]

  private[this] val signupEnabled =
    Play.current.configuration.getBoolean("signup.enabled").getOrElse(false)

  def user(id: Long): Action[Unit] = HasToken(parse.empty) { implicit request =>
    userDAO.findById(id).map(u => Ok(Json.toJson(u))).
      getOrElse(notFound("Unknown user"))
  }

  def searchByName(name: String): Action[Unit] = HasToken(parse.empty) { implicit request =>
    Ok(Json.toJson(userDAO.searchByName(name)))
  }

  def createUser(): Action[JsValue] = Action(parse.json) { implicit request =>
    expect[User] { user =>
      if(signupEnabled) {
        userDAO.findByName(user.name) match {
          case Some(_) => badRequest(s"'${user.name}' is already taken.")
          case None => {
            mailer.selfAddress.map { selfAddress =>
              mailer.sendText(selfAddress, selfAddress,
                s"New User: ${user.name}",
                s"A new user has signed up: ${user.name}")
            }
            Ok(Json.toJson(userDAO.insert(user)))
          }
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

  def resetPassword(id: Long): Action[JsValue] = Admin(parse.json) { implicit request =>
    expect[ResetPasswordRequest] { resetRequest =>
      userDAO.findById(id).map { user =>
        if(resetRequest.newPassword == resetRequest.newPasswordConfirm) {
          Ok(Json.toJson(userDAO.changePassword(user, resetRequest.newPassword)))
        } else {
          badRequest("Passwords do not match")
        }
      } getOrElse notFound("Unknown user")
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

  def changeAvatar(id: Long): Action[JsValue] = HasToken(parse.json(maxLength = avatarMaxSize)) { implicit request =>
    expect[Option[FileUpload]] { file =>
      sameUserOrAdmin(id) {

        val image = file.map(file => Image(None, file.bytes))

        Ok(Json.toJson(userDAO.setAvatar(request.user, image)))

      }("A user may only change their own avatar")
    }
  }

  def deleteUser(id: Long): Action[Unit] = HasToken(parse.empty) { implicit request =>
    sameUserOrAdmin(id) {
      val deletedUser = userDAO.findById(id)
      if(userDAO.delete(id) == 1) {

        mailer.selfAddress.map { selfAddress =>
          mailer.sendText(selfAddress, selfAddress,
            s"""User Deleted: ${deletedUser.map(_.name).getOrElse("???")}""",
            s"""A user has been deleted: ${deletedUser.map(_.name).getOrElse("???")}""")
        }

        ok("User deleted")
      } else {
        notFound("Unknown user")
      }
    }("A user may only delete their own account")
  }

  def stats(id: Long): Action[Unit] = HasToken.async(parse.empty) { implicit request =>
    userDAO.findById(id).map { user =>
      userStatsService.forUser(user).map(stats => Ok(Json.toJson(stats)))
    } getOrElse(Future.successful(notFound("Unknown user")))
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
