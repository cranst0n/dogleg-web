package controllers

import org.specs2.matcher.ThrownMessages

import scaldi.Module

import play.api.libs.json._
import play.api.test._

import test.Helpers._

import models._

import services.{ ProductionModule, UserDAO }
import services.slick.UserDAOSlick

object AuthenticationSpec extends PlaySpecification with ThrownMessages {

  "Authentication (controller)" should {

    "accept a valid login" in new FakeDoglegApplication {
      withUsers(FakeUser) { case (mod, List(user)) =>

        implicit val module = mod
        val ctrl = new Authentication
        val loginRequest = fakeLoginRequest(user.name, FakeUser.password)
        val result = call(ctrl.login(), loginRequest)

        status(result) must be equalTo(OK)
        cookies(result) get(ctrl.AuthTokenCookieKey) must beSome
      }
    }

    "reject an inactive user" in new FakeDoglegApplication {
      withUsers(FakeUser.copy(active = false)) { case (mod, List(user)) =>

        implicit val module = mod
        val ctrl = new Authentication
        val loginRequest = fakeLoginRequest(user.name,FakeUser.password)
        val result = call(ctrl.login(), loginRequest)

        status(result) must be equalTo(NOT_FOUND)
        contentAsString(result) must contain("Account is not activated")
      }
    }

    "reject an invalid login credentials" in new FakeDoglegApplication {
      withUsers(FakeUser) { case (mod, List(user)) =>

        implicit val module = mod
        val ctrl = new Authentication
        val loginRequest = fakeLoginRequest(user.name, FakeUser.password + "23")
        val result = call(ctrl.login(), loginRequest)

        status(result) must be equalTo(UNAUTHORIZED)
      }
    }

    "logout" in new FakeDoglegApplication {
      withTokenRequest(FakeUser) { (user, mod, tokenRequest) =>

        implicit val module = mod
        val ctrl = new Authentication

        val request =
          FakeRequest().withHeaders(tokenRequest.headers.toSimpleMap.toList: _*)

        val pingResult1 = call(ctrl.authUser(), request)
        status(pingResult1) must be equalTo(OK)
        val logoutResult = call(ctrl.logout(), request)
        status(logoutResult) must be equalTo(OK)
        val pingResult2 = call(ctrl.authUser(), request)
        status(pingResult2) must be equalTo(UNAUTHORIZED)
      }
    }

    "reject a ping without a token" in new FakeDoglegApplication {
      val ctrl = new Authentication()(ProductionModule())
      val result = call(ctrl.authUser(), FakeRequest())
      status(result) must be equalTo(UNAUTHORIZED)
    }

    "accept a ping with token" in new FakeDoglegApplication {
      withTokenRequest { (user, mod, tokenRequest) =>

        implicit val module = mod
        val ctrl = new Authentication
        val tokenResult = ctrl.authUser()(tokenRequest)
        status(tokenResult.run) must be equalTo(OK)
      }
    }

    "reject an admin ping from unprivileged user" in new FakeDoglegApplication {
      withTokenRequest(FakeUser) { (user, mod, tokenRequest) =>

        implicit val module = mod
        val ctrl = new Authentication
        val tokenResult = ctrl.authAdmin()(tokenRequest)
        status(tokenResult.run) must be equalTo(FORBIDDEN)
      }
    }

    "accept an admin ping from privileged user" in new FakeDoglegApplication {
      withTokenRequest(FakeUser.copy(admin = true)) { (user, mod, tokenRequest) =>

        implicit val module = mod
        val ctrl = new Authentication
        val tokenResult = ctrl.authAdmin()(tokenRequest)
        status(tokenResult.run) must be equalTo(OK)
      }
    }
  }

  val FakeUser = simpleUser

  def fakeLoginRequest(name: String, password: String) = {
    FakeRequest(
      method = POST,
      uri = controllers.routes.Authentication.login().url,
      headers = FakeHeaders(),
      body = Json.toJson(LoginCredentials(name,password))
    )
  }
}