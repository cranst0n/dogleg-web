package services.slick

import org.joda.time.DateTime

import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

import scaldi.Injectable._

import models._

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
              HoleScore(None, None, 5, 5, 2, 0, true, false,
                List(Shot(None, 1, Driver, LatLon(1, 1), LatLon(2, 2), None)),
                insertedCourse.holes(0)),
              HoleScore(None, None, 6, 6, 3, 0, false, true,
                List(Shot(None, 1, Driver, LatLon(2, 2), LatLon(3, 3), None)),
                insertedCourse.holes(1)),
              HoleScore(None, None, 2, 2, 1, 0, false, true,
                List(Shot(None, 1, Driver, LatLon(3, 3), LatLon(4, 4), None)),
                insertedCourse.holes(2)),
              HoleScore(None, None, 8, 8, 2, 1, false, false,
                List(Shot(None, 1, Driver, LatLon(4, 4), LatLon(5, 5), None)),
                insertedCourse.holes(3)),
              HoleScore(None, None, 5, 5, 3, 0, true, true,
                List(Shot(None, 1, Driver, LatLon(5, 5), LatLon(6, 6), None)),
                insertedCourse.holes(4)),
              HoleScore(None, None, 3, 3, 1, 0, true, true,
                List(Shot(None, 1, Driver, LatLon(6, 6), LatLon(7, 7), None)),
                insertedCourse.holes(5)),
              HoleScore(None, None, 3, 3, 1, 0, true, true,
                List(Shot(None, 1, Driver, LatLon(7, 7), LatLon(8, 8), None)),
                insertedCourse.holes(6)),
              HoleScore(None, None, 3, 3, 2, 0, false, true,
                List(Shot(None, 1, Driver, LatLon(8, 8), LatLon(9, 9), None)),
                insertedCourse.holes(7)),
              HoleScore(None, None, 6, 6, 3, 0, false, false,
                List(Shot(None, 1, Driver, LatLon(9, 9), LatLon(10, 10), None)),
                insertedCourse.holes(8))
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

          round.holeScores.zipWithIndex forall { case (score,index) =>
            score.shots must have size(1)
            score.shots.head.id must beSome
            score.shots.head.club must be equalTo(Driver)
            score.shots.head.locationStart must be equalTo(LatLon(index+1, index+1))
            score.shots.head.holeScoreId must be equalTo(score.id)
          }
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
              HoleScore(None, None, 5, 5, 2, 0, true, false,
                List(Shot(None, 1, Driver, LatLon(1, 1), LatLon(2, 2), None)),
                insertedCourse.holes(0)),
              HoleScore(None, None, 6, 6, 3, 0, false, true,
                List(Shot(None, 1, Driver, LatLon(2, 2), LatLon(3, 3), None)),
                insertedCourse.holes(1)),
              HoleScore(None, None, 2, 2, 1, 0, false, true,
                List(Shot(None, 1, Driver, LatLon(3, 3), LatLon(4, 4), None)),
                insertedCourse.holes(2)),
              HoleScore(None, None, 8, 8, 2, 1, false, false,
                List(Shot(None, 1, Driver, LatLon(4, 4), LatLon(5, 5), None)),
                insertedCourse.holes(3)),
              HoleScore(None, None, 5, 5, 3, 0, true, true,
                List(Shot(None, 1, Driver, LatLon(5, 5), LatLon(6, 6), None)),
                insertedCourse.holes(4)),
              HoleScore(None, None, 3, 3, 1, 0, true, true,
                List(Shot(None, 1, Driver, LatLon(6, 6), LatLon(7, 7), None)),
                insertedCourse.holes(5)),
              HoleScore(None, None, 3, 3, 1, 0, true, true,
                List(Shot(None, 1, Driver, LatLon(7, 7), LatLon(8, 8), None)),
                insertedCourse.holes(6)),
              HoleScore(None, None, 3, 3, 2, 0, false, true,
                List(Shot(None, 1, Driver, LatLon(8, 8), LatLon(9, 9), None)),
                insertedCourse.holes(7)),
              HoleScore(None, None, 6, 6, 3, 0, false, false,
                List(Shot(None, 1, Driver, LatLon(9, 9), LatLon(10, 10), None)),
                insertedCourse.holes(8))
            ),
            None, Some(11), true
          )

        val insertedRound = roundDAO.insert(round)

        val ratingToUpdate = insertedCourse.ratings.find(_.teeName == "White").
          getOrElse(fail("Unknown course rating."))

        val roundToUpdate = insertedRound.copy(
          rating = ratingToUpdate,
          holeScores = insertedRound.holeScores.map { holeScore =>
            if(holeScore.hole.number % 3 == 0) {
              holeScore.copy(
                score = holeScore.score + 1,
                shots = holeScore.shots.map(_.copy(club = Iron4))
              )
            } else {
              holeScore
            }
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
          r.holeScores(2).score must be equalTo(insertedRound.holeScores(2).score + 1)
          r.holeScores(2).shots must have size(1)
          r.holeScores(2).shots.head.club must be equalTo(Iron4)
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
