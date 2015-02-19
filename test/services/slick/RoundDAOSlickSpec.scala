package services.slick

import org.joda.time.DateTime

import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

import scaldi.Injectable._

import models.{ HoleScore, Round }

import services.{ CourseDAO, HoleScoreDAO, RoundDAO }

import test.Helpers._

object RoundDAOSlickSpec extends Specification with ThrownMessages {

  "RoundDAOSlick" should {

    "insert a new round" in new FakeDoglegApplication {
      withUsers(simpleUser) { case (mod, List(user)) =>

        implicit val module = mod

        val courseDAO = inject[CourseDAO]
        val roundDAO = inject[RoundDAO]

        val insertedCourse = courseDAO.insert(Rehoboth)
        val rating = insertedCourse.ratings.find(_.teeName == "Blue").
          getOrElse(fail("Unknown course rating"))

        val round =
          Round(None, user, insertedCourse, rating, new DateTime,
            List(
              HoleScore(None, 5, 5, 2, 0, true, false, insertedCourse.holes(0)),
              HoleScore(None, 6, 6, 3, 0, false, true, insertedCourse.holes(1)),
              HoleScore(None, 2, 2, 1, 0, false, true, insertedCourse.holes(2)),
              HoleScore(None, 8, 8, 2, 1, false, false, insertedCourse.holes(3)),
              HoleScore(None, 5, 5, 3, 0, true, true, insertedCourse.holes(4)),
              HoleScore(None, 3, 3, 1, 0, true, true, insertedCourse.holes(5)),
              HoleScore(None, 3, 3, 1, 0, true, true, insertedCourse.holes(6)),
              HoleScore(None, 3, 3, 2, 0, false, true, insertedCourse.holes(7)),
              HoleScore(None, 6, 6, 3, 0, false, false, insertedCourse.holes(8))
            ),
            None, Some(4), false
          )

        val insertedRound = roundDAO.insert(round)
        insertedRound.id must beSome

        val foundRound = roundDAO.findById(insertedRound.id.get)

        foundRound must beSome.which { round =>
          round.id must be equalTo insertedRound.id
          round.holeScores must have size round.holeScores.size
          round.handicap must beNone
          round.handicapOverride must beSome(4)
          round.official must beFalse
        }
      }
    }

    "update an existing round" in new FakeDoglegApplication {
      withUsers(simpleUser) { case (mod, List(user)) =>

        implicit val module = mod

        val courseDAO = inject[CourseDAO]
        val roundDAO = inject[RoundDAO]

        val insertedCourse = courseDAO.insert(Rehoboth)
        val rating = insertedCourse.ratings.find(_.teeName == "Blue").
          getOrElse(fail("Unknown course rating"))

        val round =
          Round(None, user, insertedCourse, rating, new DateTime,
            List(
              HoleScore(None, 5, 5, 2, 0, true, false, insertedCourse.holes(0)),
              HoleScore(None, 6, 6, 3, 0, false, true, insertedCourse.holes(1)),
              HoleScore(None, 2, 2, 1, 0, false, true, insertedCourse.holes(2)),
              HoleScore(None, 8, 8, 2, 1, false, false, insertedCourse.holes(3)),
              HoleScore(None, 5, 5, 3, 0, true, true, insertedCourse.holes(4)),
              HoleScore(None, 3, 3, 1, 0, true, true, insertedCourse.holes(5)),
              HoleScore(None, 3, 3, 1, 0, true, true, insertedCourse.holes(6)),
              HoleScore(None, 3, 3, 2, 0, false, true, insertedCourse.holes(7)),
              HoleScore(None, 6, 6, 3, 0, false, false, insertedCourse.holes(8))
            ),
            None, Some(11), true
          )

        val insertedRound = roundDAO.insert(round)

        val ratingToUpdate = insertedCourse.ratings.find(_.teeName == "White").
          getOrElse(fail("Unknown course rating."))

        val roundToUpdate = insertedRound.copy(
          rating = ratingToUpdate,
          holeScores = insertedRound.holeScores.map { holeScore =>
            if(holeScore.hole.number % 3 == 0)
              holeScore.copy(score = holeScore.score + 1)
            else
              holeScore
          },
          handicap = Some(13),
          handicapOverride = None,
          official = false
        )

        val updatedRound = roundDAO.update(roundToUpdate)

        updatedRound must beSome.which { updated =>
          updated.id must be equalTo insertedRound.id
        }

        val updatedById = updatedRound.flatMap { r =>
          r.id.map { id =>
            roundDAO.findById(id)
          }
        } getOrElse fail("Round update failed!")

        updatedById must beSome.which { r =>
          r.holeScores(2).score must be equalTo(round.holeScores(2).score + 1)
          r.handicap must beSome(13)
          r.handicapOverride must beNone
          r.official must beFalse
        }
      }
    }
  }

  lazy val Rehoboth = loadCourse("Rehoboth").
    getOrElse(fail("Load failed for Rehoboth.")).copy(approved = Option(true))
}