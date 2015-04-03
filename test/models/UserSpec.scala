package models

import org.joda.time.DateTime
import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

import play.api.libs.json._

object UserSpec extends Specification with ThrownMessages {

  "User" should {

    "never include password in JSON (implicitly)" in {
      val json = Json.toJson(User(Some(1),"user","pass","e-mail"))
      json \ "password" must be equalTo(JsString(""))
    }
  }
}