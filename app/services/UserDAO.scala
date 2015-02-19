package services

import com.lambdaworks.crypto.SCryptUtil

import models.{ Image, User }

trait UserDAO {

  def authenticate(name: String, password: String): Option[User] = {

    loadFailsafeUser // Make sure the failsafe user is there if no one else is

    findByName(name).orElse(findByEmail(name)).map { user =>
      if (SCryptUtil.check(password, user.password)) { Some(user) }
      else { None }
    } getOrElse None
  }

  def findByName(name: String): Option[User]

  def findByEmail(email: String): Option[User]

  def findById(id: Long): Option[User]

  def exists(name: String): Boolean = {
    findByName(name).isDefined
  }

  def insert(user: User): User

  def update(id: Long, newInfo: User): Option[User]

  def delete(id: Long): Int

  def changePassword(user: User, password: String): User

  def setAvatar(user: User, avatar: Option[Image]): User

  def count: Long

  private[this] def loadFailsafeUser: Option[User] = {
    count match {
      case c if c == 0 && findByName(failsafeUser.name).isEmpty => {
        Some(insert(failsafeUser))
      }
      case _ => None
    }
  }

  private[this] val failsafeUser =
    User(None, "dogleg",
      "6a4eb4f2d5e08dde448327f608255e6effe7f9ac33f2e8db8a20a09e8a7347ce792f71" +
      "ee51b0934143d7464c05dfbe2ed97b5ad816f76c493f00488d4f67ab7d",
      "dogleg@dogleg.com", true, true, TimeSource.system.nowUtc)

  protected[UserDAO] def hashPassword(pass: String): String = {
    SCryptUtil.scrypt(pass, sCryptCPUCost, sCryptMemCost, sCryptParallelization)
  }

  private[this] val sCryptCPUCost = 16384
  private[this] val sCryptMemCost = 8
  private[this] val sCryptParallelization = 1
}
