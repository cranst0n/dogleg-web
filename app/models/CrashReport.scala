package models

import org.joda.time.DateTime

import play.api.libs.json._

case class CrashReport(id: Option[Long], time: DateTime, report: JsValue)

object CrashReport {
  implicit val jsonFormat = Json.format[CrashReport]
}