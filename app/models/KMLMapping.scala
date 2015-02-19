package models

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }
import scala.xml.XML

import scaldi.{ Injectable, Injector }

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json

import services.{ ElevationService, GeoCodingService }

case class KMLPlacemark(name: String, locations: List[LatLon])

case class KMLMapping(name: String, placemarks: List[KMLPlacemark],
  geoCodingService: GeoCodingService, elevationService: ElevationService) {

  def toCourse: Future[Course] = {

    val courseFeatures: Map[Int,List[HoleFeature]] =
      placemarks.foldLeft(Map[Int,List[HoleFeature]]()) { (accum,placemark) =>

        // May be getting elevation from outside world so Try it
        Try {

          val holeFeature(holeString,featureName) = placemark.name
          val hole = holeString.toInt

          val withElevation: Future[List[LatLon]] =
            Future.sequence(placemark.locations.map(elevationService.elevation(_)))

          val feature =
            withElevation.map { points =>
              HoleFeature(featureName, points)
            }

          accum + ((hole, Await.result(feature,5.seconds) :: (accum.getOrElse(hole,List()))))

        } match {
          case Success(result) => result
          case _ => accum
        }
      }

    val courseLatLon =
      placemarks.find(_.name.toLowerCase == "home").map(_.locations.head).
        getOrElse(courseFeatures.head._2.head.coordinates.head)
    val geoCodeLookup = geoCodingService.reverseGeoCode(courseLatLon)

    val holes =
      courseFeatures.keys.toList.map { hole =>
        Hole(None,hole,None,courseFeatures(hole))
      }.sortWith(_.number < _.number)

    geoCodeLookup.map { geoCode =>
      Course(None,name, geoCode.city, geoCode.state, geoCode.country,
        holes.size, courseLatLon, holes)
    }
  }

  private[this] val holeFeature = """(\d+)-(.*?)""".r
}

object KMLMapping {

  def fromKML(kml: String, geoCodingService: GeoCodingService,
    elevationService: ElevationService): KMLMapping = {

    val xml = XML.loadString(kml)

    val folderElem = xml \ "Document" \ "Folder"
    val mappingName = (folderElem \ "name").text

    val placemarks =
      (folderElem \\ "Placemark").toList.map { placemarkElem =>
        val name = (placemarkElem \ "name").text
        val latLonString =
          ((placemarkElem \ "Point" \ "coordinates").text +
            (placemarkElem \ "LineString" \ "coordinates").text
          ).trim
        KMLPlacemark(name,parseCoordinateString(latLonString))
      }

    KMLMapping(mappingName, placemarks, geoCodingService, elevationService)
  }

  private[this] def parseCoordinateString(s: String): List[LatLon] = {
    val coordinates =
      s.trim.split(' ').map(_.split(',')).flatten.toList.map(_.toDouble)

    coordinates.sliding(3,3).toList.map {
      case lon :: lat :: alt :: Nil => Some(LatLon(lat,lon,alt))
      case _ => None
    }.flatten
  }
}
