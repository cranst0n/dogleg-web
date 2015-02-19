package models

import play.api.libs.json._

case class Image(id: Option[Long], data: Array[Byte])

object Image {

  implicit val JsonReads = new Reads[Image] {
    override def reads(jsValue: JsValue): JsResult[Image] = {
      (jsValue \ "id").asOpt[Long].map { id =>
        JsSuccess(Image(Some(id), Array()))
      } getOrElse JsError(JsPath(), "id field not found")
    }
  }

  implicit val JsonWrites = new Writes[Image] {
    override def writes(image: Image): JsValue = {
      Json.obj("id" -> image.id)
    }
  }
}
