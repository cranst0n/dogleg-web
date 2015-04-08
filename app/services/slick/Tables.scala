package services.slick

import scala.slick.jdbc.JdbcBackend.SessionDef

import org.joda.time.DateTime

import com.vividsolutions.jts.geom.{ MultiPoint, Point }

import play.api.libs.json.JsValue

import DoglegPostgresDriver.simple._

import models._

object Tables {

  lazy val crashReports = TableQuery[CrashReports]
  lazy val images = TableQuery[Images]

  lazy val users = TableQuery[Users]
  lazy val userProfiles = TableQuery[UserProfiles]

  lazy val requestedCourses = TableQuery[RequestedCourses]
  lazy val courses = TableQuery[Courses]
  lazy val courseRatings = TableQuery[CourseRatings]
  lazy val holeRatings = TableQuery[HoleRatings]
  lazy val holes = TableQuery[Holes]
  lazy val holeFeatures = TableQuery[HoleFeatures]
  lazy val rounds = TableQuery[Rounds]
  lazy val holeScores = TableQuery[HoleScores]
  lazy val shots = TableQuery[Shots]

  lazy val courseImages = TableQuery[CourseImages]
  lazy val roundImages = TableQuery[RoundImages]

  // scalastyle:off

  class CrashReports(tag: Tag) extends Table[CrashReport](tag, "crashreport") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def time = column[DateTime]("time")
    def report = column[JsValue]("report")

    def * = (id.?, time, report) <> ((CrashReport.apply _).tupled, CrashReport.unapply)
  }

  class Images(tag: Tag) extends Table[Image](tag, "images") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def data = column[Array[Byte]]("data")
    def * = (id.?, data) <> ((Image.apply _).tupled, Image.unapply)
  }

  case class DBUser(id: Option[Long], name: String, password: String,
    email: String, admin: Boolean, active: Boolean,
    created: DateTime) {

    def toUser(implicit session: SessionDef) = {

      val profile = userProfiles.filter(_.userId === id).firstOption.
        map(_.toUserProfile).getOrElse(UserProfile.empty)

      User(id, name, password, email, admin, active, created, profile)
    }
  }

  class Users(tag: Tag) extends Table[DBUser](tag, "dogleguser") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def password = column[String]("password")
    def email = column[String]("email")
    def admin = column[Boolean]("admin")
    def active = column[Boolean]("active")
    def created = column[DateTime]("created")
    def * = (id.?, name, password, email, admin, active, created) <> ((DBUser.apply _).tupled, DBUser.unapply)
  }

  case class DBUserProfile(home: Option[String], location: Option[Point],
    avatarId: Option[Long], favoriteCourseId: Option[Long],
    userId: Option[Long]) {

    def toUserProfile(implicit session: SessionDef) = {
      UserProfile(home, location.map(LatLon.fromVividPoint),
        avatarId.flatMap(id => images.filter(_.id === id).firstOption),
        courses.filter(_.id === favoriteCourseId).firstOption.map(_.toCourse)
      )
    }
  }

  class UserProfiles(tag: Tag) extends Table[DBUserProfile](tag, "dogleguserprofile") {
    def home = column[Option[String]]("home")
    def location = column[Option[Point]]("location")
    def avatarId = column[Option[Long]]("avatarid")
    def favoriteCourseId = column[Option[Long]]("favoritecourseid")
    def userId = column[Long]("userid")
    def * = (home, location, avatarId, favoriteCourseId, userId.?) <> ((DBUserProfile.apply _).tupled, DBUserProfile.unapply)
  }

  case class DBRequestedCourse(id: Option[Long], name: String, city: String,
    state: String, country: String, website: Option[String],
    comment: Option[String], created: DateTime, requestorId: Option[Long],
    fulfilledBy: Option[Long]) {

    def toRequestedCourse(implicit session: SessionDef) = {
      RequestedCourse(id, name, city, state, country, website, comment, created,
        users.filter(_.id === requestorId).firstOption.map(_.toUser),
        fulfilledBy.flatMap(id => courses.filter(_.id === id).firstOption.map(_.toCourse.summary))
      )
    }
  }

  class RequestedCourses(tag: Tag) extends Table[DBRequestedCourse](tag, "requestedcourse") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def city = column[String]("city")
    def state = column[String]("state")
    def country = column[String]("country")
    def website = column[Option[String]]("website")
    def comment = column[Option[String]]("comment")
    def created = column[DateTime]("created")
    def requestorId = column[Option[Long]]("requestorid")
    def fulfilledBy = column[Option[Long]]("fulfilledby")
    def * = (id.?, name, city, state, country, website, comment, created, requestorId, fulfilledBy) <> ((DBRequestedCourse.apply _).tupled, DBRequestedCourse.unapply)
  }

  case class DBCourse(id: Option[Long], name: String, city: String, state: String,
    country: String, numHoles: Int, exclusivity: String, phoneNumber: String,
    location: Point, creatorId: Option[Long], approved: Boolean) {

    def toCourse(implicit session: SessionDef) = {
      Course(id, name, city, state, country, numHoles,
        Exclusivity.parse(exclusivity).getOrElse(Exclusivity.Public),
        phoneNumber, LatLon.fromVividPoint(location),
        holes.filter(_.courseId === id).list.map(_.toHole).sorted,
        courseRatings.filter(_.courseId === id).list.
          map(_.toCourseRating).sorted,
        creatorId, Some(approved))
    }
  }

  class Courses(tag: Tag) extends Table[DBCourse](tag, "course") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def city = column[String]("city")
    def state = column[String]("state")
    def country = column[String]("country")
    def numHoles = column[Int]("numholes")
    def exclusivity = column[String]("exclusivity")
    def phoneNumber = column[String]("phonenumber")
    def location = column[Point]("location")
    def creatorId = column[Option[Long]]("creatorid")
    def approved = column[Boolean]("approved")
    def * = (id.?,name,city,state,country,numHoles,exclusivity,phoneNumber,location,creatorId,approved) <> ((DBCourse.apply _).tupled, DBCourse.unapply)
  }

  case class DBCourseRating(id: Option[Long], teeName: String, rating: Double,
    slope: Double, frontRating: Double, frontSlope: Double, backRating: Double,
    backSlope: Double, bogeyRating: Double, gender: String, courseId: Long) {

    def toCourseRating(implicit session: SessionDef) = {

      CourseRating(id, teeName, rating, slope, frontRating,
        frontSlope, backRating, backSlope, bogeyRating,
        Gender.parse(gender).getOrElse(Gender.Male),
        holeRatings.filter(_.ratingId === id).list.map(_.toHoleRating).sorted)
    }
  }

  class CourseRatings(tag: Tag) extends Table[DBCourseRating](tag, "courserating") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def teeName = column[String]("teename")
    def rating = column[Double]("rating")
    def slope = column[Double]("slope")
    def frontRating = column[Double]("frontrating")
    def frontSlope = column[Double]("frontslope")
    def backRating = column[Double]("backrating")
    def backSlope = column[Double]("backslope")
    def bogeyRating = column[Double]("bogeyrating")
    def gender = column[String]("gender")
    def courseId = column[Long]("courseid")
    def * = (id.?,teeName,rating,slope,frontRating,frontSlope,backRating,backSlope,bogeyRating,gender,courseId) <> ((DBCourseRating.apply _).tupled, DBCourseRating.unapply)
  }

  case class DBHoleRating(id: Option[Long], number: Int, par: Int, yardage: Int,
    handicap: Int, ratingId: Long) {

    def toHoleRating(implicit session: SessionDef) = {
      HoleRating(id, number, par, yardage, handicap)
    }
  }

  class HoleRatings(tag: Tag) extends Table[DBHoleRating](tag, "holerating") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def number = column[Int]("number")
    def par = column[Int]("par")
    def yardage = column[Int]("yardage")
    def handicap = column[Int]("handicap")
    def ratingId = column[Long]("ratingid")
    def * = (id.?, number, par, yardage, handicap, ratingId) <> ((DBHoleRating.apply _).tupled, DBHoleRating.unapply)
  }

  case class DBHole(id: Option[Long], num: Int, courseId: Long) {

    def toHole(implicit session: SessionDef) = {
      Hole(id, num, Some(courseId),
        holeFeatures.filter(_.holeId === id).list.map(_.toHoleFeature)
      )
    }
  }

  class Holes(tag: Tag) extends Table[DBHole](tag, "hole") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def number = column[Int]("number")
    def courseId = column[Long]("courseid")
    def * = (id.?, number, courseId) <> ((DBHole.apply _).tupled, DBHole.unapply)
  }

  case class DBHoleFeature(id: Option[Long], name : String, multiPoint: MultiPoint, holeId: Option[Long]) {
    def toHoleFeature = {
      val latLonList = multiPoint.getCoordinates.toList.map { p =>
        LatLon.fromVividCoordinate(p)
      }

      HoleFeature(id, name, latLonList, holeId)
    }
  }

  class HoleFeatures(tag: Tag) extends Table[DBHoleFeature](tag, "holefeature") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def coordinates = column[MultiPoint]("coordinates")
    def holeId = column[Long]("holeid")
    def * = (id.?, name, coordinates, holeId.?) <> ((DBHoleFeature.apply _).tupled, DBHoleFeature.unapply)
  }

  case class DBRound(id: Option[Long], time: DateTime, official: Boolean,
    userId: Long, courseId: Long, ratingId: Long, handicap: Option[Int],
    handicapOverride: Option[Int]) {

    def toRound(implicit session: SessionDef) = {
      Round(id,
        users.filter(_.id === userId).first.toUser,
        courses.filter(_.id === courseId).first.toCourse,
        courseRatings.filter(_.id === ratingId).first.toCourseRating,
        time,
        holeScores.filter(_.roundId === id).list.map(_.toHoleScore).sorted,
        handicap,
        handicapOverride,
        official)
    }
  }

  class Rounds(tag: Tag) extends Table[DBRound](tag, "round") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def time = column[DateTime]("time")
    def official = column[Boolean]("official")
    def userId = column[Long]("userid")
    def courseId = column[Long]("courseid")
    def ratingId = column[Long]("ratingid")
    def handicap = column[Option[Int]]("handicap")
    def handicapOverride = column[Option[Int]]("handicapoverride")
    def * = (id.?, time, official, userId, courseId, ratingId, handicap, handicapOverride) <> ((DBRound.apply _).tupled, DBRound.unapply)
  }

  case class DBHoleScore(id: Option[Long], score: Int, netScore: Int,
    putts: Int, penaltyStrokes: Int, fairwayHit: Boolean, gir: Boolean,
    roundId: Option[Long], holeId: Long) {

    def toHoleScore(implicit session: SessionDef) = {
      HoleScore(id, roundId, score, netScore, putts, penaltyStrokes, fairwayHit,
        gir,
        shots.filter(_.holeScoreId === id).list.map(_.toShot),
        holes.filter(_.id === holeId).first.toHole)
    }
  }

  class HoleScores(tag: Tag) extends Table[DBHoleScore](tag, "holescore") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def score = column[Int]("score")
    def netScore = column[Int]("netscore")
    def putts = column[Int]("putts")
    def penaltyStrokes = column[Int]("penaltystrokes")
    def fairwayHit = column[Boolean]("fairwayhit")
    def gir = column[Boolean]("gir")
    def roundId = column[Option[Long]]("roundid")
    def holeId = column[Long]("holeid")
    def * = (id.?, score, netScore, putts, penaltyStrokes, fairwayHit, gir, roundId, holeId) <> ((DBHoleScore.apply _).tupled, DBHoleScore.unapply)
  }

  case class DBShot(id: Option[Long], sequence: Int, clubId: Int,
    locationStart: Point, locationEnd: Point, holeScoreId: Option[Long]) {

    def toShot(implicit session: SessionDef) = {
      Shot(id, sequence, Club.forId(clubId),
        LatLon.fromVividPoint(locationStart),
        LatLon.fromVividPoint(locationEnd), holeScoreId)
    }
  }

  class Shots(tag: Tag) extends Table[DBShot](tag, "shot") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def sequence = column[Int]("sequence")
    def clubId = column[Int]("clubid")
    def locationStart = column[Point]("locationstart")
    def locationEnd = column[Point]("locationend")
    def holeScoreId = column[Long]("holescoreid")

    def * = (id.?, sequence, clubId, locationStart, locationEnd, holeScoreId.?) <> ((DBShot.apply _).tupled, DBShot.unapply)
  }

  case class AttachedImage(entityId: Long, imageId: Long)

  class CourseImages(tag: Tag) extends Table[AttachedImage](tag, "courseimage") {
    def courseId = column[Long]("courseid")
    def imageId = column[Long]("imageid")
    def * = (courseId,imageId) <> ((AttachedImage.apply _).tupled, AttachedImage.unapply)
  }

  class RoundImages(tag: Tag) extends Table[AttachedImage](tag, "roundimage") {
    def roundId = column[Long]("courseid")
    def imageId = column[Long]("imageid")
    def * = (roundId,imageId) <> ((AttachedImage.apply _).tupled, AttachedImage.unapply)
  }

  // scalastyle:on
}
