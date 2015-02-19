package services.slick

import org.joda.time.DateTime

import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

import models.{ RequestedCourse, User }

import test.Helpers._

object RequestedCourseDAOSlickSpec extends Specification with ThrownMessages {

  "RequestedCourseDAOSlick" should {

    "insert a request" in new FakeDoglegApplication {
      withUsers(simpleUser) { case (module, List(user)) =>

        val dao = new RequestedCourseDAOSlick()(module)
        val request = fakeRequest(user)
        val inserted = dao.insert(request)

        inserted.id must beSome(1)
        inserted must be equalTo request.copy(id = inserted.id)
      }
    }

    "find a request by ID" in new FakeDoglegApplication {
      withUsers(simpleUser) { case (module, List(user)) =>

        val dao = new RequestedCourseDAOSlick()(module)
        val request = fakeRequest(user)
        val inserted = dao.insert(request)

        inserted.id.map { id =>
          dao.findById(id) must beSome.which { found =>
            found must be equalTo inserted
          }
        } getOrElse fail("Inserted request has no ID!")
      }
    }

    "find requests submitted by a user" in new FakeDoglegApplication {
      withUsers(simpleUser, simpleUser) { case (module, List(user1, user2)) =>

        val dao = new RequestedCourseDAOSlick()(module)

        val user1Requests = (1 to 10).map(_ => dao.insert(fakeRequest(user1)))
        val user2Requests = (1 to 6).map(_ => dao.insert(fakeRequest(user2)))

        user1.id must beSome.which { id =>
          dao.forUser(id, 20, 0) must haveSize(10)
        }

        user2.id must beSome.which { id =>
          dao.forUser(id, 20, 0) must haveSize(6)
        }

        dao.forUser(1234, 20, 0) must beEmpty
      }
    }

    "delete an existing request" in new FakeDoglegApplication {
      withUsers(simpleUser) { case (module, List(user)) =>

        val dao = new RequestedCourseDAOSlick()(module)
        val request = fakeRequest(user)
        val inserted = dao.insert(request)

        inserted.id.map { id =>
          dao.delete(id) must be equalTo 1
          dao.delete(id) must be equalTo 0
        } getOrElse fail("Inserted request has no ID!")
      }
    }

    "list requests" in new FakeDoglegApplication {
      withUsers(simpleUser) { case (module, List(user)) =>

        val dao = new RequestedCourseDAOSlick()(module)
        val request = fakeRequest(user)

        val inserted = (1 to 10).map(i => dao.insert(request))

        dao.list(false) must beEmpty // fulfilled requests
        dao.list(true) must haveSize(10) // outstanding requests
        dao.list(true, 3) must haveSize(3)
        dao.list(true, 10, 5) must haveSize(5)
      }
    }

    "update a request" in new FakeDoglegApplication {
      withUsers(simpleUser) { case (module, List(user)) =>

        val dao = new RequestedCourseDAOSlick()(module)
        val request = fakeRequest(user)

        val inserted = dao.insert(request)


        inserted.id.map { id =>
          val updateWith = request.copy(
            name = "new name",
            city = "new city",
            state = "new state",
            country = "new country",
            website = None,
            comment = None
          )

          dao.update(id, updateWith) must beSome.which { updated =>
            dao.findById(id) must beSome.which { found =>
              found must be equalTo updated.copy(id = found.id)
            }
          }

        } getOrElse fail("Inserted request has no ID!")
      }
    }
  }


  def fakeRequest(user: User) = {
    RequestedCourse(None, "name", "city", "state", "country",
    Some("www.fake.com"), Some("A comment!"), new DateTime(1234), Some(user),
    None)
  }
}
