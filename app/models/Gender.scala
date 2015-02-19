package models

import utils.json.JsonUtils._

object Gender extends Enumeration {

  type Gender = Value

  val Male, Female = Value

  def parse(str: String): Option[Gender] = {
    str.toLowerCase match {
      case "male" => Some(Male)
      case "female" => Some(Female)
      case _ => None
    }
  }

  implicit val jsonFormat = enumFormat(Gender)
}
