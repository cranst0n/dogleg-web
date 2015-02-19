package controllers

import org.specs2.matcher.ThrownMessages

import play.api.libs.json._
import play.api.test._

import test.Helpers._

import models._

import services.{ ProductionModule, UserDAO }
import services.slick.UserDAOSlick

object ApplicationSpec extends PlaySpecification with ThrownMessages {

  "Application (controller)" should {

    "provide a landing page" in new FakeDoglegApplication {

      val ctrl = new Application()(ProductionModule())
      val result = call(ctrl.index(), FakeRequest())

      status(result) must be equalTo(OK)
      contentAsString(result) must contain("javascripts/main.js")
    }

    "provide javascript routes" in new FakeDoglegApplication {

      val ctrl = new Application()(ProductionModule())
      val result = call(ctrl.jsRoutes(), FakeRequest())

      status(result) must be equalTo(OK)

      val resultString = contentAsString(result)

      resultString must contain("controllers.Application.index")
    }
  }
}