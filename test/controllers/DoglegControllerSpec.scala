package controllers

import org.specs2.matcher.ThrownMessages

import play.api.mvc.Action
import play.api.libs.json._
import play.api.test._

import test.Helpers._

import models._

object DoglegControllerSpec extends PlaySpecification with ThrownMessages {

  "DoglegController" should {

    "expect JSON POST" in new FakeDoglegApplication {

      val controller = new DoglegController {
        def expectTest = Action(parse.json) { implicit request =>
          expect[LoginCredentials] { credentials =>
            Ok(Json.obj("message" -> "It worked!"))
          }
        }
      }

      val result = controller.expectTest()(validJsonRequest)
      status(result) must be equalTo(OK)
    }

    "complain on a bad JSON POST" in new FakeDoglegApplication {
      val controller = new DoglegController {
        def expectTest = Action(parse.json) { implicit request =>
          expect[LoginCredentials] { credentials =>
            Ok(Json.obj("message" -> "It worked!"))
          }
        }
      }

      val result = controller.expectTest()(invalidJsonRequest)
      status(result) must be equalTo(BAD_REQUEST)
    }

  }

  val validJsonRequest =
    FakeRequest(
      method = POST,
      uri = controllers.routes.Authentication.login().url,
      headers = FakeHeaders(),
      body = Json.toJson(LoginCredentials("name","password"))
    )

  val invalidJsonRequest =
    FakeRequest(
      method = POST,
      uri = controllers.routes.Authentication.login().url,
      headers = FakeHeaders(),
      body = Json.obj("bad" -> "form")
    )
}
