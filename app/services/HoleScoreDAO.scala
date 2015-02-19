package services

import models.{ HoleScore, Round }

trait HoleScoreDAO {

  def insert(round: Round): List[HoleScore]

  def update(round: Round): List[HoleScore]

  def forRound(roundId: Long): List[HoleScore]
}
