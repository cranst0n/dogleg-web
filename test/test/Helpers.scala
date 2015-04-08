package test

import scala.io.Source
import scala.slick.jdbc.StaticQuery.interpolation
import scala.util.Random

import org.joda.time.DateTime
import org.specs2.execute.{ AsResult, Result }
import org.specs2.matcher.ThrownMessages
import org.specs2.mock.Mockito

import scaldi.{ Injectable, Module }

import play.api.{ Application, GlobalSettings, Play }
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import play.api.libs.json._
import play.api.mvc.AnyContentAsEmpty
import play.api.test._
import play.api.test.Helpers._

import models._

import services.{ ProductionModule, UserDAO }
import services.slick.UserDAOSlick

object Helpers extends Mockito {

  class FakeDoglegApplication(options: Map[String,String] = Map.empty)
    extends WithApplication(
      FakeApplication(additionalConfiguration = testDBConfig ++ options)
    ) with Injectable {
      override def around[T: AsResult](t: => T): Result = super.around {
        try {
          t
        } finally {
          cleanDB
        }
      }
    }

  def DoglegTestApp[T](block: Module => T, options: Map[String,String] = Map.empty): T = {
    running(FakeApplication(additionalConfiguration = testDBConfig ++ options)) {
      try {
        block(services.ProductionModule())
      } finally {
        cleanDB(play.api.Play.current)
      }
    }
  }

  def withTokenRequest[T](block: (User,Module,FakeRequest[_]) => T): Option[T] = {
    withTokenRequest(simpleUser)(block)
  }

  def withTokenRequest[T](user: User)(block: (User,Module,FakeRequest[_]) => T): Option[T] = {

    implicit val module = ProductionModule()

    val userDAO = new UserDAOSlick
    val createdUser = userDAO.insert(user)

    module.bind[UserDAO] to userDAO

    val ctrl = new controllers.Authentication {
      implicit val bindingModule = module }

    val loginRequest = {
      FakeRequest(
        method = POST,
        uri = controllers.routes.Authentication.login().url,
        headers = FakeHeaders(),
        body = Json.toJson(controllers.LoginCredentials(createdUser.name,user.password))
      )
    }

    val result = ctrl.login()(loginRequest)

    (cookies(result) get(ctrl.AuthTokenCookieKey)).map(_.value).map { token =>

      val tokenHeaders = FakeHeaders(Seq(ctrl.AuthTokenHeader -> Seq(token)))
      val tokenRequest = FakeRequest(
        method = GET,
        uri = controllers.routes.Application.index().url,
        headers = tokenHeaders,
        body = ""
      )

      block(createdUser, module, tokenRequest)
    }
  }

  def withUsers[T](users: User*)(block: (Module, List[User]) => T) = {

    implicit val module = ProductionModule()

    val userDAO = new UserDAOSlick

    module.bind[UserDAO] to userDAO

    val insertedUsers = users.map(userDAO.insert).toList

    block(module, insertedUsers)
  }

  def loadJson(filePath: String) = {
    val source = Source.fromURL(getClass.getResource(filePath))
    Json.parse(source.getLines.mkString("\n"))
  }

  def loadCourse(courseName: String): Option[models.Course] = {
    loadJson(s"/courses/$courseName.json").validate[models.Course].asOpt
  }

  def simpleUser: User = {
    User(None, "user", "password", "e@mail.com", false, true)
  }

  def randomRound(user: User, course: Course, rating: CourseRating): Round = {

    val holeScores =
      course.holes.zip(rating.holeRatings).map { case (hole,holeRating) =>

        val score = holeRating.par + Random.nextInt(5) - 1
        val netScore = 0
        val putts = (Random.nextInt(3) + 1).min(score - 1)

        val penaltyStrokes =
          if(score - putts > holeRating.par) {
            if(Random.nextInt(10) < 4 ) 1 else 0
          } else {
            0
          }

        val fairwayHit =
          score match {
            case s if(score <= (holeRating.par + 1) && Random.nextInt(5) > 2) => true
            case s => score <= holeRating.par
          }

        val gir = score - putts <= holeRating.par - 2

        HoleScore(None, None, score, netScore, putts,
          penaltyStrokes, fairwayHit, gir, Nil, hole)
      }

    Round(None, user, course, rating, new DateTime(), holeScores,
      None, None, true)
  }

  private[this] def cleanDB(implicit app: Application) {
    DB withSession { implicit session =>
      sqlu"""drop schema public cascade""".execute
      sqlu"""create schema public""".execute
    }
  }

  private[this] val testDBConfig =
    Map(
      // Play-Flyway compliance
      "db.default.driver"           -> "org.postgresql.Driver",
      "db.default.url"              -> "jdbc:postgresql://localhost/doglegtest",
      "db.default.user"             -> "dogleg",
      "db.default.password"         -> "dogleg",
      // HikariCP compliance
      "db.default.driverClassName"  -> "org.postgresql.Driver",
      "db.default.jdbcUrl"          -> "jdbc:postgresql://localhost/doglegtest",
      "db.default.username"         -> "dogleg"
    )
}