package services

import scaldi.Module

import controllers._

import services._
import services.google._
import services.slick._

class ProductionModule extends Module {

  bind[TimeSource] to TimeSource.system
  bind[UUIDGenerator] to new DefaultUUIDGenerator

  bind[GeoCodingService] to new GoogleGeoCodingService
  // Google elevation service is particular about the request rate
  // so it's disabled by default.
  bind[ElevationService] to new MockElevationService

  bind[HandicapService] to new DefaultHandicapService

  bind[CrashReportDAO] to new CrashReportDAOSlick
  bind[ImageDAO] to new ImageDAOSlick

  bind[UserDAO] to new UserDAOSlick
  bind[RequestedCourseDAO] to new RequestedCourseDAOSlick
  bind[HoleFeatureDAO] to new HoleFeatureDAOSlick
  bind[HoleDAO] to new HoleDAOSlick
  bind[HoleRatingDAO] to new HoleRatingDAOSlick
  bind[CourseRatingDAO] to new CourseRatingDAOSlick
  bind[CourseDAO] to new CourseDAOSlick

  bind[HoleScoreDAO] to new HoleScoreDAOSlick
  bind[RoundDAO] to new RoundDAOSlick

}

object ProductionModule {
  def apply(): ProductionModule = new ProductionModule
}

class WebModule extends Module {

  binding to new Application
  binding to new Authentication
  binding to new Courses
  binding to new CrashReports
  binding to new Images
  binding to new RequestedCourses
  binding to new Rounds
  binding to new Users
}

object WebModule {
  def apply(): WebModule = new WebModule
}
