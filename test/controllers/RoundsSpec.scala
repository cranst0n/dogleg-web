package controllers

import org.joda.time.DateTime
import org.specs2.matcher.ThrownMessages

import play.api.libs.json._
import play.api.test._

import models.Round

import services.{ CourseDAO, RoundDAO }

import test.Helpers._

object RoundsSpec extends PlaySpecification with ThrownMessages {

  "Rounds (controller)" should {

    "create a Round" in new FakeDoglegApplication {
      withTokenRequest { (user, mod, tokenRequest) =>

        implicit val module = mod
        val courseDAO = inject[CourseDAO]
        val roundDAO = inject[RoundDAO]

        val insertedCourse = courseDAO.insert(Rehoboth)

        val fakeRound =
          randomRound(user, insertedCourse, insertedCourse.ratings(0))

        val createRoundRequest =
          RoundCreateRequest(fakeRound.course.id.get, fakeRound.rating.id.get,
            fakeRound.time.getMillis, fakeRound.holeScores,
            fakeRound.handicapOverride, fakeRound.official)

        val request =
          FakeRequest(
            method = POST,
            uri = controllers.routes.Rounds.createRound().url,
            headers = tokenRequest.headers,
            body = Json.toJson(createRoundRequest)
          )

        val ctrl = new Rounds
        val result = call(ctrl.createRound(), request)

        status(result) must be equalTo OK
        contentAsJson(result).validate[Round].asOpt.map { round =>
          roundDAO.findById(round.id.getOrElse(-1)) must beSome.which { found =>
            found.score must be equalTo fakeRound.score
            found.par must be equalTo fakeRound.par
            found.holeScores must have size fakeRound.holeScores.size
          }
        } getOrElse("Invalid JSON returned!")
      }
    }

    "update a Round" in new FakeDoglegApplication {
      withTokenRequest { (user, mod, tokenRequest) =>

        implicit val module = mod
        val courseDAO = inject[CourseDAO]
        val roundDAO = inject[RoundDAO]


        val insertedCourse = courseDAO.insert(Rehoboth)
        val originalRound = roundDAO.insert(
          randomRound(user, insertedCourse, insertedCourse.ratings.head))

        val newRating = insertedCourse.ratings.last
        val newTime = new DateTime(1234)

        val toUpdate =
          originalRound.copy(
            rating = newRating,
            time = newTime,
            holeScores =
              originalRound.holeScores.updated(2,
                originalRound.holeScores(2).copy(score = 11))
          )

        val ctrl = new Rounds
        val request =
          FakeRequest(
            method = POST,
            uri = controllers.routes.Rounds.updateRound().url,
            headers = tokenRequest.headers,
            body = Json.toJson(toUpdate)
          )

        val result = call(ctrl.updateRound(), request)

        status(result) must be equalTo OK

        roundDAO.findById(originalRound.id.get) must beSome.which { r =>
          r.rating.id must be equalTo newRating.id
          r.time must be equalTo newTime
          r.holeScores(2).score must be equalTo 11
        }
      }
    }


    "return round list" in new FakeDoglegApplication {
      withTokenRequest { (user, mod, tokenRequest) =>

        implicit val module = mod
        val courseDAO = inject[CourseDAO]
        val roundDAO = inject[RoundDAO]


        val insertedCourse = courseDAO.insert(Rehoboth)

        val round1 = roundDAO.insert(
          randomRound(user, insertedCourse, insertedCourse.ratings(0)))
        val round2 = roundDAO.insert(
          randomRound(user, insertedCourse, insertedCourse.ratings(0)))

        val ctrl = new Rounds
        val result = call(ctrl.list(20,0), FakeRequest())
        contentAsJson(result).validate[List[Round]].asOpt.map { rounds =>
          rounds must have size 2
        } getOrElse("Invalid JSON returned!")

        val offsetResult = call(ctrl.list(20,1), FakeRequest())
        contentAsJson(offsetResult).validate[List[Round]].asOpt.map { rounds =>
          rounds must have size 1
          rounds.flatMap(_.id) must contain(round2.id.get)
        } getOrElse("Invalid JSON returned!")
      }
    }

    "return round info" in new FakeDoglegApplication {
      withTokenRequest { (user, mod, tokenRequest) =>

        implicit val module = mod
        val courseDAO = inject[CourseDAO]
        val roundDAO = inject[RoundDAO]


        val insertedCourse = courseDAO.insert(Rehoboth)

        val round1 = roundDAO.insert(
          randomRound(user, insertedCourse, insertedCourse.ratings(0)))

        val ctrl = new Rounds
        val request1 =
          FakeRequest(
            method = GET,
            uri = controllers.routes.Rounds.info(round1.id.getOrElse(-1)).url,
            headers = tokenRequest.headers,
            body = ""
          )

        val result1 = call(ctrl.info(round1.id.getOrElse(-1)), request1)
        status(result1) must be equalTo OK

        val badResult = call(ctrl.info(1234), request1)
        status(badResult) must be equalTo NOT_FOUND
      }
    }

    "delete a round" in new FakeDoglegApplication {
      withTokenRequest { (user, mod, tokenRequest) =>

        implicit val module = mod
        val courseDAO = inject[CourseDAO]
        val roundDAO = inject[RoundDAO]


        val insertedCourse = courseDAO.insert(Rehoboth)

        val round1 = roundDAO.insert(
          randomRound(user, insertedCourse, insertedCourse.ratings(0)))

        val ctrl = new Rounds
        val request =
          FakeRequest(
            method = DELETE,
            uri = controllers.routes.Rounds.deleteRound(round1.id.getOrElse(-1)).url,
            headers = tokenRequest.headers,
            body = ""
          )

        val result = call(ctrl.deleteRound(round1.id.getOrElse(-1)), request)
        status(result) must be equalTo OK

        roundDAO.list(user,20,0) must beEmpty
      }
    }
  }

  lazy val Rehoboth = loadCourse("Rehoboth").
      getOrElse(fail("Load course failed for 'Rehoboth'"))
}