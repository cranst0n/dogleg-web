package controllers

import scala.concurrent.Future
import scala.concurrent.duration._

import scaldi.{ Injectable, Injector }

import play.api.cache.Cache
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._

import models.User

import services.UserDAO

trait Security extends DoglegController with Injectable {

  self: Controller =>

  implicit val injector: Injector

  type Token = String

  lazy val userDAO = inject[UserDAO]

  implicit def app: play.api.Application = play.api.Play.current

  val AuthTokenHeader = "X-XSRF-TOKEN"
  val AuthTokenCookieKey = "XSRF-TOKEN"
  val AuthTokenUrlKey = "auth"

  lazy val CacheExpiration = app.configuration.getInt("token.expiration").
    getOrElse(3.day.toSeconds.toInt) // 3 day default

  case class TokenRequest[A](token: Token, user: User,
    private val request: Request[A]) extends WrappedRequest(request)

  implicit class ResultWithToken(result: Result) {

    def renewToken(implicit request: TokenRequest[_]): Result = {
      request.user.id.map { userId =>
        Cache.set(request.token, userId, CacheExpiration)
      }
      result.withCookies(
        Cookie(AuthTokenCookieKey, request.token, None, httpOnly = false))
    }

    def discardingToken(implicit request: Request[_]): Result = {
      maybeToken(request).map { token =>
        Cache.remove(token)
        result.discardingCookies(DiscardingCookie(name = AuthTokenCookieKey))
      } getOrElse result
    }
  }

  object Admin extends ActionBuilder[TokenRequest] {
    def invokeBlock[A](request: Request[A],
      block: (TokenRequest[A]) => Future[Result]): Future[Result] = {
      handleTokenRequest(request, block, _.admin)
    }
  }

  object HasToken extends ActionBuilder[TokenRequest] {
    def invokeBlock[A](request: Request[A],
      block: (TokenRequest[A]) => Future[Result]): Future[Result] = {
      handleTokenRequest(request, block)
    }
  }

  private[this] def handleTokenRequest[A](request: Request[A],
    block: (TokenRequest[A]) => Future[Result],
    userAuth: (User) => Boolean = _ => true) = {

    maybeToken(request) flatMap { token =>
      Cache.getAs[Long](token) map { userid =>
        userDAO.findById(userid).map { user =>
          if(userAuth(user)) {
            implicit val tokenRequest = TokenRequest(token,user,request)
            block(tokenRequest).map(_.renewToken)
          } else {
            Future.successful(forbidden("Unauthorized"))
          }
        } getOrElse Future.successful(unauthorized("Unknown user"))
      }
    } getOrElse Future.successful(unauthorized("No Token", "Expired or missing token."))
  }

  private[this] def maybeToken[A](request: Request[A]): Option[Token] = {
    request.headers.get(AuthTokenHeader).
        orElse(request.getQueryString(AuthTokenUrlKey))
  }
}
