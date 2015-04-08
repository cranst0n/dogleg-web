package services

import models.{ HoleScore, Shot }

trait ShotDAO {

  def insert(holeScore: HoleScore): List[Shot]

  def update(holeScore: HoleScore): List[Shot]

  def forHoleScore(holeScoreId: Long): List[Shot]
}
