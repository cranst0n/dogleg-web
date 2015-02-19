package services

import scala.concurrent.Future

import models.{ GeoCode, LatLon }

trait GeoCodingService {
  def geoCode(geoCode: GeoCode): Future[LatLon]
  def reverseGeoCode(latLon: LatLon): Future[GeoCode]
}

class MockGeoCodingService extends GeoCodingService {
  override def geoCode(geoCode: GeoCode): Future[LatLon] = {
    Future.successful(LatLon(0,0))
  }

  override def reverseGeoCode(latLon: LatLon): Future[GeoCode] = {
    Future.successful(GeoCode("Newport","RI","USA"))
  }
}
