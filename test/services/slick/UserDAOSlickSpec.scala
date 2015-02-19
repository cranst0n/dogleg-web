package services.slick

import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

import models.User

import test.Helpers._

object UserDAOSlickSpec extends Specification with ThrownMessages {

  "UserDAOSlick" should {

    "insert a new user" in DoglegTestApp { implicit module =>
      val dao = new UserDAOSlick
      val insertedUser = dao.insert(SimpleUser)

      insertedUser.id must beSome
    }

    "find a user by name" in DoglegTestApp { implicit module =>
      val dao = new UserDAOSlick
      val insertedUser = dao.insert(SimpleUser)

      insertedUser.id.map { id =>
        dao.findById(id) must beSome(insertedUser)
      } getOrElse fail("Inserted user not assigned an ID!")
    }

    "find a user by e-mail" in DoglegTestApp { implicit module =>
      val dao = new UserDAOSlick
      val insertedUser = dao.insert(SimpleUser)

      dao.findByEmail(insertedUser.email) must beSome(insertedUser)
    }

    "determine if a username already exists" in DoglegTestApp { implicit module =>
      val dao = new UserDAOSlick
      val insertedUser = dao.insert(SimpleUser)

      dao.exists(insertedUser.name) must beTrue
      dao.exists("Not" + insertedUser.name) must beFalse
    }

    "change a users password" in DoglegTestApp { implicit module =>

      val dao = new UserDAOSlick
      val insertedUser = dao.insert(SimpleUser)

      dao.authenticate(insertedUser.name,
        SimpleUser.password) must beSome(insertedUser)

      val newPassword = "ribs"

      dao.changePassword(insertedUser, newPassword)
      dao.authenticate(insertedUser.name,SimpleUser.password) must beNone
      dao.authenticate(insertedUser.name,newPassword) must beSome
    }
  }

  val SimpleUser = simpleUser
}
