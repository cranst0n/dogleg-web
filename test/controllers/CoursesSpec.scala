package controllers

import scala.io.Source

import org.specs2.matcher.ThrownMessages

import scaldi.Module
import scaldi.Injectable._

import play.api.Play
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.mvc._
import play.api.mvc.MultipartFormData.FilePart
import play.api.test._

import test.Helpers._

import models._

import services.{ CourseDAO, ProductionModule, UserDAO }
import services.slick.CourseDAOSlick

object CoursesSpec extends PlaySpecification with ThrownMessages {

  "Courses (controller)" should {

    "provide course summary list" in DoglegTestApp { implicit module =>

      val courseDAO = inject[CourseDAO]

      val course1 = courseDAO.insert(Rehoboth.copy(approved = Some(true)))
      val course2 = courseDAO.insert(PoquoyBrook.copy(approved = Some(true)))

      val ctrl = new Courses
      val result = call(ctrl.list(None,None,10,0,true), FakeRequest())
      val jsonResult = contentAsJson(result)

      status(result) must be equalTo OK

      jsonResult.validate[List[CourseSummary]] match {
        case JsSuccess(list, _) => {
          list must have size 2
        }
        case JsError(errors) => {
          fail(s"Course list failed: $errors.mkString")
        }
      }

      val offsetResult = call(ctrl.list(None,None,10,1,true), FakeRequest())
      val offsetJsonResult = contentAsJson(offsetResult)

      offsetJsonResult.validate[List[CourseSummary]] match {
        case JsSuccess(list, _) => {
          list must have size 1
          list.head.name must be equalTo "Poquoy Brook"
        }
        case JsError(errors) => fail(s"Course list failed: ${errors.mkString}")
      }
    }

    "provide course information" in new FakeDoglegApplication {
      withCourses(Rehoboth,PoquoyBrook) { case (mod, List(r,p)) =>

        implicit val module = mod
        val ctrl = new Courses

        val result = call(ctrl.info(p.id.get), FakeRequest())
        val jsonResult = contentAsJson(result)

        status(result) must be equalTo OK

        jsonResult.validate[Course] match {
          case JsSuccess(course,_) => {
            course.name must be equalTo p.name
          }
          case JsError(errors) => {
            fail(s"Course info failed: ${errors.mkString}")
          }
        }

        val nonCourseResult = call(ctrl.info(1234), FakeRequest())
        status(nonCourseResult) must be equalTo NOT_FOUND
      }
    }

    "create a course" in new FakeDoglegApplication {
      withTokenRequest { (user, mod, tokenRequest) =>

        implicit val module = mod
        val courseDAO = inject[CourseDAO]

        val ctrl = new Courses
        val request =
          FakeRequest(
            method = POST,
            uri = controllers.routes.Courses.createCourse().url,
            headers = tokenRequest.headers,
            body = Json.toJson(Rehoboth)
          )

        val result = call(ctrl.createCourse(), request)

        status(result) must be equalTo OK
        val returnedCourse =
          contentAsJson(result).validate[Course].asOpt.
          getOrElse(fail("Creating course didn't return new course"))

        courseDAO.findById(returnedCourse.id.getOrElse(-1)) must beSome
        returnedCourse.creatorId must be equalTo(user.id)
        returnedCourse.approved must beSome(false)
      }
    }

    "proved list of unapproved courses" in new FakeDoglegApplication {
      val approved = Rehoboth.copy(approved = Option(true))
      withCourses(Rehoboth,approved,PoquoyBrook) { case (mod, List(a,b,c)) =>

        implicit val module = mod
        val ctrl = new Courses
        val result = call(ctrl.list(None,None,20,0,false), FakeRequest())

        status(result) must be equalTo OK

        contentAsJson(result).validate[List[CourseSummary]] match {
          case JsSuccess(list,_) => {
            list.map(_.name) must contain(allOf(a.name,c.name))
          } case JsError(errors) => {
            fail(s"Course unapproved list failed: ${errors.mkString}")
          }
        }
      }
    }

    "approve a course" in new FakeDoglegApplication {

      val admin = User(Some(3),"admin","pass","email",true,true)

      withTokenRequest(admin) { case (user, mod, tokenRequest) =>

        implicit val module = mod
        val courseDAO = inject[CourseDAO]
        val courses = List(Rehoboth,PoquoyBrook).map(courseDAO.insert)

        courses must have size(2)

        val ctrl = new Courses

        contentAsJson(ctrl.list(None,None,20,0,true)(FakeRequest()).run).validate[List[CourseSummary]] match {
          case JsSuccess(list,_) => list must beEmpty
          case JsError(errors) => fail("Course list should be empty.")
        }

        val request =
          FakeRequest(
            method = PUT,
            uri = controllers.routes.Courses.approve().url,
            headers = tokenRequest.headers,
            body = Json.toJson(
              CourseApproveRequest(courses.head.id.getOrElse(-1),
                courses.head.copy(name = "Batman"))
            )
          )

        val result = call(ctrl.approve(), request)

        status(result) must be equalTo OK
        contentAsJson(ctrl.list(None,None,20,0,true)(FakeRequest()).run).validate[List[CourseSummary]] match {
          case JsSuccess(list,_) => {
            list must have size 1
            list.head.name must be equalTo("Batman")
          }
          case JsError(errors) => fail("Course list should be empty.")
        }
      }
    }

    "delete a course" in new FakeDoglegApplication {
      val admin = new User(Some(3),"admin","pass","email",true,true)
      withTokenRequest(admin) { case (user, mod, tokenRequest) =>

        implicit val module = mod
        val courseDAO = inject[CourseDAO]

        val courses =
          List(Rehoboth.copy(approved=Some(true)),
            PoquoyBrook.copy(approved=Some(true))).map(courseDAO.insert)

        courseDAO.list(CourseDAO.DefaultListSize, 0) must have size 2

        val ctrl = new Courses
        val result = ctrl.delete(courses.head.id.getOrElse(-1))(tokenRequest)

        status(result.run) must be equalTo OK
        courseDAO.list(CourseDAO.DefaultListSize, 0) must have size 1
      }
    }

    "parse an uploaded JSON file" in new FakeDoglegApplication {
      Play.getExistingFile(s"test/resources/courses/Rehoboth.json").map { courseFile =>

        val fileContent = Source.fromFile(courseFile).getLines.mkString
        val upload = FileUpload(courseFile.length, "json", courseFile.getName, fileContent)

        implicit val module = ProductionModule()
        val ctrl = new Courses
        val request =
          FakeRequest(
            method = POST,
            uri = controllers.routes.Courses.parseFile().url,
            headers = FakeHeaders(),
            body = Json.toJson(upload)
          )

        val result = call(ctrl.parseFile(), request)

        status(result) must be equalTo OK
      } getOrElse fail("Failed to load Rehoboth JSON file.")
    }

    "provide raw course JSON" in new FakeDoglegApplication {
      withCourses(Rehoboth) { case (mod, List(r)) =>

        implicit val module = mod
        val ctrl = new Courses

        val result = call(ctrl.raw(r.id.get), FakeRequest())
        val jsonResult = contentAsJson(result)

        status(result) must be equalTo OK

        jsonResult.validate[Course] match {
          case JsSuccess(rawCourse,_) => {
            Json.toJson(rawCourse) must be equalTo Json.toJson(Rehoboth)
          }
          case JsError(errors) => {
            fail(s"Course info failed: ${errors.mkString}")
          }
        }
      }
    }

    lazy val Rehoboth = loadCourse("Rehoboth").
      getOrElse(fail("Load course failed for 'Rehoboth'"))

    lazy val PoquoyBrook = loadCourse("PoquoyBrook").
      getOrElse(fail("Load course failed for 'PoquoyBrook'"))
  }

  private[this] def withCourses[T](courses: Course*)(block: (Module,List[Course]) => T) = {

    implicit val module = ProductionModule()

    val courseDAO = new CourseDAOSlick
    module.bind[CourseDAO] to courseDAO

    val insertedCourses = courses.map(courseDAO.insert).toList

    block(module,insertedCourses)
  }
}
