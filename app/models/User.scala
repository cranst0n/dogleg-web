package models

import org.joda.time.DateTime

import play.api.libs.json._

case class User(id: Option[Long], name: String, password: String,
  email: String, admin: Boolean = false, active: Boolean = false,
  created: DateTime = DateTime.now, profile: UserProfile = UserProfile.empty)

object User {

  private[this] val passwordlessWrites = new Writes[User] {
    override def writes(u: User): JsValue = {
      Json.toJson(u.copy(password = ""))(defaultWrites)
    }
    private[this] val defaultWrites = Json.writes[User]
  }

  implicit val jsonFormat = Format(Json.reads[User], passwordlessWrites)

}
