package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class HoleFeature(name: String, coordinates: List[LatLon], holeId: Option[Long] = None) {

  lazy val estimateCenter: LatLon = {
    coordinates match {
      case Nil => LatLon(0,0)
      case head :: Nil => head
      case head :: tail => {
        val latLonSum =
          coordinates.reduce { (ll1,ll2) =>
            LatLon(ll1.latitude + ll2.latitude,
              ll1.longitude + ll2.longitude, ll1.altitude + ll2.altitude)
          }

        LatLon(latLonSum.latitude/coordinates.size,
          latLonSum.longitude/coordinates.size,
          latLonSum.altitude/coordinates.size)
      }
    }
  }
}

object HoleFeature {
  implicit val jsonFormat = Json.format[HoleFeature]
}
