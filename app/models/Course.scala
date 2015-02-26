package models

import play.api.libs.json.Json

import Exclusivity._

case class Course(id: Option[Long], name: String, city: String, state: String,
  country: String, numHoles: Int, exclusivity: Exclusivity, phoneNumber: String,
  location: LatLon, holes: List[Hole] = Nil, ratings: List[CourseRating] = Nil,
  creatorId: Option[Long] = None, approved: Option[Boolean] = None) {

  def hole(num: Int): Option[Hole] = {
    holes.find(_.number == num)
  }

  lazy val estimateYardage: Int = {
    holes.foldLeft(0) { _ + _.estimateYardage }
  }

  lazy val estimatePar: Int = {
    holes.foldLeft(0) { _ + _.estimatePar }
  }

  lazy val summary = {
    CourseSummary(id, name, city, state, country, numHoles,
      ratings.headOption.map(_.par).getOrElse(0), exclusivity, phoneNumber,
      location, creatorId, approved)
  }

  // Strip out ID's etc. so only the most basic information is included.
  lazy val raw = {
    this.copy(
      id = None,
      holes = holes.map { h =>
        h.copy(
          id = None,
          courseId = None,
          features = h.features.map { hf =>
            hf.copy(id = None, holeId = None)
          }
        )
      },
      ratings = ratings.map { cr =>
        cr.copy(
          id = None,
          holeRatings = cr.holeRatings.map { hr =>
            hr.copy(id = None)
          }
        )
      },
      creatorId = None,
      approved = None
    )
  }
}

object Course {
  implicit val jsonFormat = Json.format[Course]
}

case class CourseSummary(id: Option[Long], name: String, city: String, state: String,
  country: String, numHoles: Int, par: Int, exclusivity: Exclusivity,
  phoneNumber: String, location: LatLon, creatorId: Option[Long] = None,
  approved: Option[Boolean] = None)

object CourseSummary {
  implicit val jsonFormat = Json.format[CourseSummary]
}
