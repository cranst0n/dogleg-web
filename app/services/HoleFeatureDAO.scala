package services

import models.{ Hole, HoleFeature }

trait HoleFeatureDAO {

  def forHole(holeId: Long): List[HoleFeature]

  def insert(hole: Hole): List[HoleFeature]

  def update(hole: Hole): List[HoleFeature]
}
