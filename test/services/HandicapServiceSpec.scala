package services

import play.api.test.PlaySpecification

import org.joda.time.DateTime

import models.Round

import test.Helpers._

object HandicapServiceSpec extends PlaySpecification {

  "HandicapService" should {

    "give no handicap with insufficient rounds" in {
      new DefaultHandicapService().handicap(Nil) must beNone
    }

    "give accurate handicap with minimum number of rounds (full rounds)" in {

      val hcpService = new DefaultHandicapService()

      hcpService.handicap(mockedRounds(5, 18)) must beSome(1.9)   // Only uses lowest differential
      hcpService.handicap(mockedRounds(7, 18)) must beSome(2.9)   // Uses lowest (2)
      hcpService.handicap(mockedRounds(10, 18)) must beSome(3.8)  // Uses lowest (3)
      hcpService.handicap(mockedRounds(12, 18)) must beSome(4.8)  // Uses lowest (4)
      hcpService.handicap(mockedRounds(14, 18)) must beSome(5.8)  // Uses lowest (5)
      hcpService.handicap(mockedRounds(15, 18)) must beSome(6.7)  // Uses lowest (6)
      hcpService.handicap(mockedRounds(17, 18)) must beSome(7.7)  // Uses lowest (7)
      hcpService.handicap(mockedRounds(18, 18)) must beSome(8.6)  // Uses lowest (8)
      hcpService.handicap(mockedRounds(19, 18)) must beSome(9.6)  // Uses lowest (9)
      hcpService.handicap(mockedRounds(20, 18)) must beSome(10.6) // Uses lowest (10)
      hcpService.handicap(mockedRounds(50, 18)) must beSome(10.6) // Uses lowest (10)
    }

    "give accurate handicap with minimum number of rounds (half rounds)" in {

      val hcpService = new DefaultHandicapService()

      // Need at least 10 half rounds
      hcpService.handicap(mockedRounds(9, 9)) must beNone

       // Averages the lowest (2) half rounds together to make one full round
      hcpService.handicap(mockedRounds(10, 9)) must beSome(2.9)
    }

    "give accurate handicap with mixed half/full rounds" in {

      val hcpService = new DefaultHandicapService()

      val rounds = List(
        mockedRound(0, 9),
        mockedRound(1, 18),
        mockedRound(2, 18),
        mockedRound(3, 18),
        mockedRound(4, 18),
        mockedRound(5, 18)
      )

      val rounds2 = mockedRound(0, 9) :: rounds

      val rounds3 = mockedRound(0, 9) :: mockedRound(100, 18) :: rounds

      hcpService.handicap(rounds) must beSome(1.9)
      hcpService.handicap(rounds2) must beSome(0)
      hcpService.handicap(rounds3) must beSome(1.0)
    }
  }

  def mockedRounds(numRounds: Int, numHoles: Int) = {
    (1 to numRounds).map { i =>
      mockedRound(i, numHoles)
    }.toList
  }

  def mockedRound(i: Int, numHoles: Int) = {
    val mocked = mock[Round]
    mocked.numHoles returns numHoles
    mocked.handicapDifferential returns i * 2
    mocked.time returns new DateTime(i)
    mocked.official returns true
    mocked
  }
}
