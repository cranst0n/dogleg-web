package models

import play.api.libs.json._
import org.cvogt.play.json.Jsonx

case class UserStats(user: User, frequentCourses: List[CourseSummary],
  autoHandicap: Int, totalRounds: Int,
  lowGross9Hole: Int, lowGross18Hole: Int, averageGross18Hole: Int,
  lowNet9Hole: Int, lowNet18Hole: Int, averageNet18Hole: Int,
  aces: Int, birdieStreak: Int, parStreak: Int,
  lowPutts18Hole: Int, mostBirdies18Hole: Int, mostPars18Hole: Int,
  averagePuttPerHole: Double,
  averageEaglesPerRound: Double, totalEagles: Int,
  averageBirdiesPerRound: Double, totalBirdies: Int,
  averageParsPerRound: Double, totalPars: Int,
  par3Average: Double, par4Average: Double, par5Average: Double
)

object UserStats {
  implicit val jsonFormat = Jsonx.formatCaseClass[UserStats]
}
