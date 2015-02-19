package models

import play.api.libs.json.Json

case class GeoCode(city: String, state: String, country: String)

object GeoCode {
  implicit val geoCodeFormat = Json.format[GeoCode]

  def empty: GeoCode = GeoCode("", "", "")
}
