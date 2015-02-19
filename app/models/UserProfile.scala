package models

import org.joda.time.DateTime

import play.api.libs.json._

case class UserProfile(home: Option[String], location: Option[LatLon],
  avatar: Option[Image], favoriteCourse: Option[Course])

object UserProfile {

  val empty = UserProfile(None, None, None, None)

  implicit val jsonFormat = Json.format[UserProfile]
}
