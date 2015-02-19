package controllers

import org.apache.commons.codec.binary.Base64

import play.api.libs.json._

case class FileUpload(filesize: Long, filetype: String,
  filename: String, content: String) {

  lazy val bytes = Base64.decodeBase64(content)
}

object FileUpload {
  implicit val jsonFormat = Json.format[FileUpload]
}
