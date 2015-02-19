package models

import org.joda.time.DateTime

import play.api.libs.json._

case class RequestedCourse(id: Option[Long], name: String, city: String,
  state: String, country: String, website: Option[String],
  comment: Option[String], created: DateTime, requestor: Option[User],
  fulfilledBy: Option[CourseSummary])

object RequestedCourse {
  implicit val jsonFormat = Json.format[RequestedCourse]
}
