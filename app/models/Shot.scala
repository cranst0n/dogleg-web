package models

import play.api.libs.json.Json

case class Shot(id: Option[Long], sequence: Int, club: Club,
  locationStart: LatLon, locationEnd: LatLon, holeScoreId: Option[Long])

object Shot {
  implicit val jsonFormat = Json.format[Shot]
}