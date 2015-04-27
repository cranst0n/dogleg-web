package models

import play.api.libs.json._
import org.cvogt.play.json.Jsonx

case class UserStats(user: User, frequentCourses: List[CourseSummary],
  autoHandicap: Int, totalRounds: Int,
  lowGross9Hole: Int, lowGross18Hole: Int, averageGross18Hole: Double,
  lowNet9Hole: Int, lowNet18Hole: Int, averageNet18Hole: Double,
  fairwayHitPercentage: Double, girPercentage: Double,
  grossScrambling: Double, netScrambling: Double,
  grossAces: Int, grossBirdieStreak: Int, grossParStreak: Int,
  netAces: Int, netBirdieStreak: Int, netParStreak: Int,
  fewestPutts18Hole: Int,
  grossMostBirdies18Hole: Int, grossMostPars18Hole: Int,
  netMostBirdies18Hole: Int, netMostPars18Hole: Int,
  averagePuttPerHole: Double, averagePenaltiesPerRound: Double,
  grossAverageEaglesPerRound: Double, grossEagles: Int,
  grossAverageBirdiesPerRound: Double, grossBirdies: Int,
  grossAverageParsPerRound: Double, grossPars: Int,
  netAverageEaglesPerRound: Double, netEagles: Int,
  netAverageBirdiesPerRound: Double, netBirdies: Int,
  netAverageParsPerRound: Double, netPars: Int,
  grossPar3Average: Double, grossPar4Average: Double, grossPar5Average: Double,
  netPar3Average: Double, netPar4Average: Double, netPar5Average: Double
)

object UserStats {
  implicit val jsonFormat = Jsonx.formatCaseClass[UserStats]
}
