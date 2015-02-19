package controllers

import org.specs2.matcher.ThrownMessages

import play.api.libs.json._
import play.api.test._

import test.Helpers._

import models._

import services.{ CourseDAO, ProductionModule, UserDAO }

object UsersSpec extends PlaySpecification with ThrownMessages {

  "Users (controller)" should {

    "provide user information" in new FakeDoglegApplication {
      withTokenRequest(FakeUser) { (user, module, tokenRequest) =>

        val ctrl = new Users()(module)
        val userResult = ctrl.user(user.id.getOrElse(-1))(tokenRequest)
        val nonUserResult = ctrl.user(1234)(tokenRequest)

        status(userResult.run) must be equalTo(OK)
        status(nonUserResult.run) must be equalTo(NOT_FOUND)
      }
    }

    "create a new user" in new FakeDoglegApplication(Map("signup.enabled" -> "true")) {
      val ctrl = new Users()(ProductionModule())

      val request =
        FakeRequest(
          method = POST,
          uri = controllers.routes.Users.createUser().url,
          headers = FakeHeaders(),
          body = Json.toJson(FakeUser)
        )

      val result = call(ctrl.createUser,request)
      val resultJson = contentAsJson(result)

      status(result) must be equalTo(OK)

      resultJson.validate[User] match {
        case JsSuccess(user, _) => {
          user.id must beSome(1)
        }
        case JsError(_) => fail("User creation failed")
      }
    }

    "update a user" in new FakeDoglegApplication {
      withTokenRequest(FakeUser) { (user,module,tokenRequest) =>
        val ctrl = new Users()(module)

        val updateRequest =
          FakeRequest(
            method = PUT,
            uri = controllers.routes.Users.updateUser(user.id.getOrElse(-1)).url,
            headers = tokenRequest.headers,
            body = Json.toJson(FakeUser.copy(name="ANewName!",email="new@email.go"))
          )

        val result = ctrl.updateUser(user.id.getOrElse(-1))(updateRequest)
        val resultJson = contentAsJson(result)

        status(result) must be equalTo(OK)
        resultJson.validate[User] match {
          case JsSuccess(user, _) => {
            user.name must be equalTo("ANewName!")
          }
          case JsError(_) => fail("User update failed")
        }
      }
    }

    "update a user profile" in new FakeDoglegApplication {
      withTokenRequest(FakeUser) { (user,mod,tokenRequest) =>

        implicit val module = mod
        val courseDAO = inject[CourseDAO]
        val rehoboth = courseDAO.insert(Rehoboth)

        val ctrl = new Users()

        val newProfile =
          UpdateProfileRequest(
            Some("my home"),
            Some(rehoboth.summary)
          )

        val updateRequest =
          FakeRequest(
            method = PUT,
            uri = controllers.routes.Users.updateProfile(user.id.getOrElse(-1)).url,
            headers = tokenRequest.headers,
            body = Json.toJson(newProfile)
          )

        val result = ctrl.updateProfile(user.id.getOrElse(-1))(updateRequest)
        val resultJson = contentAsJson(result)

        status(result) must be equalTo(OK)
        resultJson.validate[User] match {
          case JsSuccess(user, _) => {
            user.profile.home must be equalTo(newProfile.home)
            user.profile.favoriteCourse must beSome.which { course =>
              course.id must be equalTo(newProfile.favoriteCourse.get.id)
            }
          }
          case JsError(_) => fail("User update failed")
        }
      }
    }

    "change a user's password" in new FakeDoglegApplication {
      withTokenRequest(FakeUser) { (user,mod,tokenRequest) =>

        implicit val module = mod
        val userDAO = inject[UserDAO]
        val ctrl = new Users()(module)

        val newPassword = "1234567890"

        val changePasswordRequest =
          FakeRequest(
            method = PUT,
            uri = controllers.routes.Users.changePassword(user.id.get).url,
            headers = tokenRequest.headers,
            body = Json.toJson(ChangePasswordRequest(FakeUser.password,newPassword,newPassword))
          )

        val result = ctrl.changePassword(user.id.get)(changePasswordRequest)

        status(result) must be equalTo(OK)
        userDAO.authenticate(user.name, newPassword) must beSome
      }
    }

    "delete a user" in new FakeDoglegApplication {
      withTokenRequest(FakeUser) { (user, module, tokenRequest) =>
        val ctrl = new Users()(module)
        val result = ctrl.deleteUser(user.id.getOrElse(-1))(tokenRequest).run
        status(result) must be equalTo(OK)
      }
    }

    "fail to delete another user" in new FakeDoglegApplication {
      withTokenRequest(FakeUser) { (user, mod, tokenRequest) =>

        implicit val module = mod
        val userDAO = inject[UserDAO]
        val anotherUser = userDAO.insert(FakeUser)

        val ctrl = new Users()(module)
        val result = ctrl.deleteUser(anotherUser.id.get)(tokenRequest).run
        status(result) must be equalTo(FORBIDDEN)
      }
    }

    "allow an administrator to delete another user" in new FakeDoglegApplication {
      withTokenRequest(FakeUser.copy(admin = true)) { (adminUser, mod, tokenRequest) =>

        implicit val module = mod
        val userDAO = inject[UserDAO]
        val anotherUser = userDAO.insert(FakeUser)

        userDAO.count must be equalTo(2)

        val ctrl = new Users()(module)
        val result = ctrl.deleteUser(anotherUser.id.get)(tokenRequest).run
        status(result) must be equalTo(OK)

        userDAO.count must be equalTo(1)
      }
    }
  }

  val FakeUser = User(None,"anon","password","e@mail.com",false,true)
  val Rehoboth = loadCourse("Rehoboth").
    getOrElse(fail("Load course failed for 'Rehoboth'"))
}
