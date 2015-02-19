package utils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future, ExecutionContext }
import scala.concurrent.duration._

import scaldi.{ Injectable, Injector }

import play.api.libs.json.Json

import models._

import services.ElevationService

class AddCourseElevation(implicit val injector: Injector) extends Injectable {

  lazy val elevationService = inject[ElevationService]

  def addElevation(course: Course, forceUpdate: Boolean = false)
    (implicit ec: ExecutionContext, timeout: Duration): Course = {

    val locationWithElevation =
      Await.result(elevationService.elevation(course.location), timeout)

    Thread.sleep(elevationService.pausePerRequest.toMillis)

    val holesWithElevation =
      course.holes.map { hole =>

        val featuresWithElevation: List[HoleFeature] =
          hole.features.map { feature =>
            val coordinates: Future[List[LatLon]] =
              Future.sequence(
                feature.coordinates.map { feature =>
                  if(feature.altitude.abs < 1e-5 || forceUpdate) {
                    elevationService.elevation(feature)
                  } else {
                    Future.successful(feature)
                  }
                }
              )

            Thread.sleep(elevationService.pausePerRequest.toMillis)
            feature.copy(coordinates = Await.result(coordinates, timeout))
          }

        hole.copy(features = featuresWithElevation)
      }

    course.copy(
      location = locationWithElevation,
      holes = holesWithElevation
    )
  }
}

object AddCourseElevation {

  def toCourse(course: Course, forceUpdate: Boolean = false)
    (implicit injector: Injector): Course = {

    implicit val timeout = 30.seconds
    new AddCourseElevation().addElevation(course, forceUpdate)
  }
}
