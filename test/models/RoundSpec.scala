package models

import org.joda.time.DateTime

import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

import play.api.Play
import play.api.Play.current
import play.api.libs.json._

import test.Helpers._

object RoundSpec extends Specification with ThrownMessages {

  "Round" should {

    "calculate aggregates" in {

      val round =
        Round(Some(1), simpleUser, Rehoboth, RehobothBlues, new DateTime(),
          List(
            HoleScore(None, Some(1), 5, 5, 2, 0, true, false, Nil, Hole(None, 1, None)),
            HoleScore(None, Some(1), 6, 6, 3, 0, false, true, Nil, Hole(None, 2, None)),
            HoleScore(None, Some(1), 2, 2, 1, 0, false, true, Nil, Hole(None, 3, None)),
            HoleScore(None, Some(1), 8, 8, 2, 1, false, false, Nil, Hole(None, 4, None)),
            HoleScore(None, Some(1), 5, 5, 3, 0, true, true, Nil, Hole(None, 5, None)),
            HoleScore(None, Some(1), 3, 3, 1, 0, true, true, Nil, Hole(None, 6, None)),
            HoleScore(None, Some(1), 3, 3, 1, 0, true, true, Nil, Hole(None, 7, None)),
            HoleScore(None, Some(1), 3, 3, 2, 0, false, true, Nil, Hole(None, 8, None)),
            HoleScore(None, Some(1), 6, 6, 3, 0, false, false, Nil, Hole(None, 9, None))
          ),
          None, Some(18), true
        )

      round.numHoles must be equalTo 9
      round.fullRound must beFalse
      round.front9 must beTrue
      round.back9 must beFalse

      round.score must be equalTo 41
      round.putts must be equalTo 18
      round.penaltyStrokes must be equalTo 1

      round.handicapDifferential must be closeTo(4.7912,1e-3)

      round.scoreRatings must have size 9

      val fakeFullRound =
        round.copy(holeScores = round.holeScores ::: round.holeScores)

      fakeFullRound.fullRound must beTrue
      fakeFullRound.handicapDifferential must closeTo(9.6596, 1e-3)
    }
  }

  lazy val Rehoboth = loadCourse("Rehoboth").
    getOrElse(fail("Load course failed for 'Rehoboth'"))

  lazy val RehobothBlues = Rehoboth.ratings.find(_.teeName == "Blue").
    getOrElse(fail("Rehoboth Blue tees not found."))
}
