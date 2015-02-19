package controllers

import org.joda.time.DateTime
import org.specs2.matcher.ThrownMessages

import play.api.libs.json._
import play.api.test._

import models.{ RequestedCourse, User }

import services.{ CourseDAO, RequestedCourseDAO }

import test.Helpers._

object RequestedCoursesSpec extends PlaySpecification with ThrownMessages {

  "RequestedCourses (controller)" should {

    "create a request" in new FakeDoglegApplication {
      withTokenRequest { (user, mod, tokenRequest) =>

        implicit val module = mod
        val requestedCourseDAO = inject[RequestedCourseDAO]

        val courseRequest = CourseRequest("name", "city", "state", "country",
          Some("website"), Some("comment"))

        val ctrl = new RequestedCourses
        val request =
          FakeRequest(
            method = POST,
            uri = controllers.routes.RequestedCourses.createRequest().url,
            headers = tokenRequest.headers,
            body = Json.toJson(courseRequest)
          )

        val result = call(ctrl.createRequest(), request)

        status(result) must be equalTo OK
        contentAsJson(result).validate[RequestedCourse].asOpt.map { cr =>
          requestedCourseDAO.findById(cr.id.getOrElse(-1)) must beSome.which { found =>
            found.name must be equalTo courseRequest.name
            found.city must be equalTo courseRequest.city
            found.state must be equalTo courseRequest.state
            found.country must be equalTo courseRequest.country
            found.website must be equalTo courseRequest.website
            found.comment must be equalTo courseRequest.comment
          }
        } getOrElse fail("Invalid JSON returned!")
      }
    }

    "list course requests" in new FakeDoglegApplication {
      withTokenRequest { (user, mod, tokenRequest) =>

        implicit val module = mod
        val requestedCourseDAO = inject[RequestedCourseDAO]

        val courseRequests =
          (1 to 10).map(i => requestedCourseDAO.insert(fakeCourseRequest(user)))

        val ctrl = new RequestedCourses
        val request =
          FakeRequest(
            method = GET,
            uri = controllers.routes.RequestedCourses.list().url,
            headers = tokenRequest.headers,
            body = ""
          )

        val result = call(ctrl.list(10,1), request)

        status(result) must be equalTo OK
        contentAsJson(result).validate[List[RequestedCourse]].asOpt.map { list =>
          list must haveSize(9)
        } getOrElse fail("Invalid JSON returned!")
      }
    }

    "provide request information" in new FakeDoglegApplication {
      withTokenRequest { (user, mod, tokenRequest) =>

        implicit val module = mod
        val requestedCourseDAO = inject[RequestedCourseDAO]

        val insertedRequest = requestedCourseDAO.insert(fakeCourseRequest(user))
        val id = insertedRequest.id.getOrElse(fail("Inserted request has no ID!"))

        val ctrl = new RequestedCourses
        val request =
          FakeRequest(
            method = PUT,
            uri = controllers.routes.RequestedCourses.info(id).url,
            headers = tokenRequest.headers,
            body = ""
          )

        val result = call(ctrl.info(id), request)
        val notFoundResult = call(ctrl.info(-1), request)

        status(result) must be equalTo OK
        status(notFoundResult) must be equalTo NOT_FOUND
      }
    }

    "provide a user's requests" in new FakeDoglegApplication {
      withTokenRequest { (user, mod, tokenRequest) =>

        implicit val module = mod
        val requestedCourseDAO = inject[RequestedCourseDAO]

        val insertedRequests = (1 to 5).map { i =>
          requestedCourseDAO.insert(fakeCourseRequest(user))
        }

        val ctrl = new RequestedCourses
        val request =
          FakeRequest(
            method = GET,
            uri = controllers.routes.RequestedCourses.forUser(20, 0).url,
            headers = tokenRequest.headers,
            body = ""
          )

        val result = call(ctrl.forUser(20, 0), request)

        status(result) must be equalTo OK
      }
    }

    "fulfill a request" in new FakeDoglegApplication {
      withTokenRequest(simpleUser.copy(admin = true)) { (user, mod, tokenRequest) =>

        implicit val module = mod
        val courseDAO = inject[CourseDAO]

        val insertedCourse = courseDAO.insert(course)

        val requestedCourseDAO = inject[RequestedCourseDAO]
        val insertedRequest = requestedCourseDAO.insert(fakeCourseRequest(user))

        val courseId = insertedCourse.id.getOrElse(fail("Inserted request has no ID!"))
        val requestId = insertedRequest.id.getOrElse(fail("Inserted request has no ID!"))

        requestedCourseDAO.list(true) must haveSize(1)

        val ctrl = new RequestedCourses
        val request =
          FakeRequest(
            method = GET,
            uri = controllers.routes.RequestedCourses.fulfill(requestId, courseId).url,
            headers = tokenRequest.headers,
            body = ""
          )

        val result = call(ctrl.fulfill(requestId, courseId), request)

        status(result) must be equalTo OK

        requestedCourseDAO.list(true) must beEmpty
      }
    }

    "delete a request" in new FakeDoglegApplication {
      withTokenRequest(simpleUser.copy(admin = true)) { (user, mod, tokenRequest) =>

        implicit val module = mod
        val requestedCourseDAO = inject[RequestedCourseDAO]

        val insertedRequest = requestedCourseDAO.insert(fakeCourseRequest(user))
        val id = insertedRequest.id.getOrElse(fail("Inserted request has no ID!"))

        val ctrl = new RequestedCourses
        val request =
          FakeRequest(
            method = GET,
            uri = controllers.routes.RequestedCourses.deleteRequest(id).url,
            headers = tokenRequest.headers,
            body = ""
          )

        val notFoundResult = call(ctrl.deleteRequest(-1), request)
        val result = call(ctrl.deleteRequest(id), request)

        status(notFoundResult) must be equalTo NOT_FOUND
        status(result) must be equalTo OK
      }
    }
  }

  def fakeCourseRequest(user: User) = {
    RequestedCourse(None, "name", "city", "state", "country",
    Some("website"), Some("comment"), new DateTime(1234), Some(user), None)
  }

  lazy val course =
    loadCourse("Rehoboth").getOrElse(fail("Load failed for Rehoboth."))
}