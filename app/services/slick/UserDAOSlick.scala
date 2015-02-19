package services.slick

import scaldi.{ Injectable, Injector }

import play.api.Play.current
import play.api.db.slick._

import DoglegPostgresDriver.simple._
import Tables._

import models.{ Image, User }

import services._

class UserDAOSlick(implicit val injector: Injector)
  extends UserDAO with Injectable {

  lazy val timeSource = inject[TimeSource]
  lazy val uuidGenerator = inject[UUIDGenerator]

  lazy val geoCodingService = inject[GeoCodingService]

  implicit lazy val imageDAO = inject[ImageDAO]

  override def findByName(name: String): Option[User] = {
    DB withSession { implicit session =>
      users.filter(_.name === name).firstOption.map(_.toUser)
    }
  }

  override def findByEmail(email: String): Option[User] = {
    DB withSession { implicit session =>
      users.filter(_.email === email).firstOption.map(_.toUser)
    }
  }

  override def findById(id: Long): Option[User] = {
    DB withSession { implicit session =>
      users.filter(_.id === id).firstOption.map(_.toUser)
    }
  }

  override def insert(user: User): User = {
    DB withSession { implicit session =>

      val profileWithAvatar =
        user.profile.copy(
          avatar = user.profile.avatar.map(imageDAO.insert)
        )

      users returning users.map(_.id) into ((dbUser, id) => {

        userProfiles += DBUserProfile(user.profile.home, user.profile.location,
          profileWithAvatar.avatar.flatMap(_.id),
          user.profile.favoriteCourse.flatMap(_.id), id)

        dbUser.copy(id = id).toUser
    }) += DBUser(user.id, user.name, hashPassword(user.password), user.email,
        user.admin, user.active, user.created)
    }
  }

  override def update(id: Long, newInfo: User): Option[User] = {
    DB withSession { implicit session =>

      userProfiles.filter(_.userId === id).
        map(p => (p.home, p.location, p.avatarId, p.favoriteCourseId)).
        update((newInfo.profile.home, newInfo.profile.location,
          newInfo.profile.avatar.flatMap(_.id),
          newInfo.profile.favoriteCourse.flatMap(_.id)
        ))

      // Password isn't changed here. Only when explicity wanted via
      // changePassword() function
      users.filter(_.id === id).
        map(u => (u.name,u.email,u.admin,u.active)).
        update((newInfo.name, newInfo.email,
          newInfo.admin, newInfo.active))

      findById(id)
    }
  }

  override def delete(id: Long): Int = {
    DB withSession { implicit session =>
      users.filter(_.id === id).delete
    }
  }

  override def changePassword(user: User, password: String): User = {
    DB withSession { implicit session =>
      val hashed = hashPassword(password)
      users.filter(_.id === user.id).map(_.password).update(hashed)
      user.copy(password = hashed)
    }
  }

  override def count: Long = {
    DB withSession { implicit session =>
      users.filter(_.active).length.run
    }
  }

  override def setAvatar(user: User, avatar: Option[Image]): User = {
    DB withSession { implicit session =>
      avatar match {
        case Some(image) => {
          val insertedImage = imageDAO.insert(image)
          userProfiles.filter(_.userId === user.id).map(_.avatarId).
            update(insertedImage.id)
        }
        case None => {
          userProfiles.filter(_.userId === user.id).map(_.avatarId).
            update((None))
        }
      }

      user.copy(profile = user.profile.copy(avatar = avatar))
    }
  }
}
