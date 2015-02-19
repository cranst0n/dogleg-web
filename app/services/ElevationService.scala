package services

import scala.concurrent.Future
import scala.concurrent.duration._

import models.LatLon

trait ElevationService {
  def elevation(latLon: LatLon): Future[LatLon]

  def pausePerRequest: Duration
}

class MockElevationService extends ElevationService {
  override def elevation(latLon: LatLon): Future[LatLon] = {
    Future.successful(latLon)
  }

  override lazy val pausePerRequest: Duration = 0.seconds
}
