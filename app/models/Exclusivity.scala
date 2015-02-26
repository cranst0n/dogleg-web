package models

import utils.json.JsonUtils._

object Exclusivity extends Enumeration {

  type Exclusivity = Value

  val Public, Municipal, SemiPrivate, Private = Value

  def parse(str: String): Option[Exclusivity] = {
    str.toLowerCase match {
      case "public" => Some(Public)
      case "municipal" => Some(Municipal)
      case "semiprivate" => Some(SemiPrivate)
      case "private" => Some(Private)
      case _ => None
    }
  }

  implicit val jsonFormat = enumFormat(Exclusivity)
}
