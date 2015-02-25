package services.slick

import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

import models.Course

import services.CourseDAO

import test.Helpers._

object CourseDAOSlickSpec extends Specification with ThrownMessages {

  "CourseDAOSlick" should {

    "insert a new course" in DoglegTestApp { implicit module =>

      val dao = new CourseDAOSlick
      val insertedCourse = dao.insert(approvedCourse)

      approvedCourse.id must beNone
      insertedCourse.id must beSome(1)
    }

    "update a course" in DoglegTestApp { implicit module =>

      val dao = new CourseDAOSlick
      val originalCourse = dao.insert(approvedCourse)

      val courseToUpdate = originalCourse.copy(
        name = "A", city = "B", state = "C", country = "D",
        numHoles = 11,
        holes = originalCourse.holes.take(11),
        ratings = originalCourse.ratings.take(2),
        approved = Option(false)
      )

      val updatedCourseOpt = dao.update(courseToUpdate)
      updatedCourseOpt must beSome

      val updatedById = dao.findById(originalCourse.id.getOrElse(-1))
      updatedById must beSome.which { updatedCourse =>
        updatedCourse.id must be equalTo originalCourse.id
        updatedCourse.name must be equalTo courseToUpdate.name
        updatedCourse.city must be equalTo courseToUpdate.city
        updatedCourse.state must be equalTo courseToUpdate.state
        updatedCourse.country must be equalTo courseToUpdate.country
        updatedCourse.numHoles must be equalTo courseToUpdate.numHoles
        updatedCourse.holes must have size(courseToUpdate.holes.size)
        updatedCourse.ratings.map(_.teeName) must contain(exactly("Blue", "White"))
        updatedCourse.approved must beSome(false)
      }
    }

    "find a course by ID" in DoglegTestApp { implicit module =>

      val dao = new CourseDAOSlick
      val insertedCourse = dao.insert(approvedCourse)

      dao.findById(insertedCourse.id.getOrElse(-1)) must beSome
      dao.findById(insertedCourse.id.map(_+1).getOrElse(1234)) must beNone
    }

    "delete a course" in DoglegTestApp { implicit module =>

      val dao = new CourseDAOSlick

      dao.delete(1) must be equalTo(0)
      val insertedCourse = dao.insert(approvedCourse)
      insertedCourse.id.map { id =>
        dao.delete(id) must be equalTo(1)
      } getOrElse fail("Course should have been assigned an ID.")
    }

    "list courses" in DoglegTestApp { implicit module =>

      val dao = new CourseDAOSlick

      dao.list(CourseDAO.DefaultListSize, 0) must beEmpty

      for(i <- 1 to 10) { dao.insert(approvedCourse) }

      dao.list(CourseDAO.DefaultListSize, 0) must have size(10)
      dao.list(4, 0) must have size(4)

      val paginated = dao.list(4,2)
      paginated must have size(4)
      paginated.head.id must beSome(3)
    }

    "list unapproved courses" in DoglegTestApp { implicit module =>

      val dao = new CourseDAOSlick

      dao.unapproved(CourseDAO.DefaultListSize, 0) must beEmpty
      for(i <- 1 to 3) { dao.insert(approvedCourse) }
      dao.unapproved(CourseDAO.DefaultListSize, 0) must beEmpty

      for(i <- 1 to 2) { dao.insert(unapprovedCourse) }
      dao.unapproved(CourseDAO.DefaultListSize, 0) must have size(2)
    }

    "approve an unapproved course" in DoglegTestApp { implicit module =>

      val dao = new CourseDAOSlick

      val unapproved = dao.insert(unapprovedCourse)

      dao.unapproved(CourseDAO.DefaultListSize, 0) must have size(1)
      dao.approve(
        unapproved.id.getOrElse(-1)) must beSome.which(_.approved == Some(true))

      dao.unapproved(CourseDAO.DefaultListSize, 0) must beEmpty
    }

    "search for courses" in DoglegTestApp { implicit module =>

      val dao = new CourseDAOSlick

      val insertedIds = List("Alpha", "Bravo", "Charlie", "Johnny Bravo", "arlie").map { name =>
        dao.insert(searchCourse(approvedCourse, name)).id
      }

      dao.search("alpha", 10, 0).map(_.id) must contain(insertedIds(0))
      dao.search("Bravo", 10, 0).map(_.id) must contain(exactly(insertedIds(1), insertedIds(3)))
      dao.search("arlie", 10, 0).map(_.id) must contain(exactly(insertedIds(2), insertedIds(4)))
      dao.search("bogus", 10, 0).map(_.id) must beEmpty
    }
  }

  private[this] def searchCourse(course: Course, name: String) = {
    course.copy(
      name = name,
      city = name,
      state = name,
      country = name
    )
  }

  lazy val unapprovedCourse =
    loadCourse("Rehoboth").getOrElse(fail("Load failed for Rehoboth."))
  lazy val approvedCourse =
    unapprovedCourse.copy(approved = Option(true))
}
