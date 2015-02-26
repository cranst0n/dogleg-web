package controllers

import scaldi.Injector

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.mvc._
import play.api.cache.Cache

import services.UUIDGenerator

case class LoginCredentials(username: String, password: String)

object LoginCredentials {

  implicit val JsonReads = (
    (__ \ "username").read[String](minLength[String](2)) ~
    (__ \ "password").read[String](minLength[String](2))
  )((username, password) => LoginCredentials(username, password))

  implicit val JsonWrites = Json.writes[LoginCredentials]
}

class Authentication(implicit val injector: Injector) extends DoglegController with Security {

  lazy val uuidGenerator = inject[UUIDGenerator]

  /**
   * Log-in a user. Expects the credentials in the body in JSON format.
   *
   * Set the cookie [[AuthTokenCookieKey]] to have AngularJS set the X-XSRF-TOKEN in the HTTP
   * header.
   *
   * @return The token needed for subsequent requests
   */
  def login(): Action[JsValue] = Action(parse.json) { implicit request =>
    request.body.validate[LoginCredentials].fold(
      errors => {
        badRequest("Invalid username/password", errors.mkString)
      },
      credentials => {
        userDAO.authenticate(credentials.username, credentials.password).fold {
          unauthorized("User not registered")
        } { user =>
          if(user.active) {
            val token = uuidGenerator.newUUID.toString
            user.id.map { userId =>
              Cache.set(token, userId, CacheExpiration)
            }
            Ok(Json.obj("token" -> token)).withCookies(
              Cookie(AuthTokenCookieKey, token, None, httpOnly = false))
          } else {
            notFound("Account is not activated")
          }
        }
      }
    )
  }

  /**
   * Log-out a user. Invalidates the authentication token.
   *
   * Discard the cookie [[AuthTokenCookieKey]] to have AngularJS no longer set the
   * X-XSRF-TOKEN in HTTP header.
   */
  def logout(): Action[Unit] = Action(parse.empty) { implicit request =>
    ok("Logged out").discardingToken
  }

  def authUser(): Action[Unit] = HasToken(parse.empty) { implicit request =>
    Ok(Json.toJson(request.user))
  }

  def authAdmin(): Action[Unit] = Admin(parse.empty) { implicit request =>
    Ok(Json.toJson(request.user))
  }
}
