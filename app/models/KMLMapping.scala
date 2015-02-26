package models

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, PrettyPrinter, XML }

import scaldi.{ Injectable, Injector }

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json

import models.Exclusivity._

import services.{ ElevationService, GeoCodingService }

case class KMLPlacemark(name: String, locations: List[LatLon])

case class KMLMapping(name: String, placemarks: List[KMLPlacemark]) {

  def toCourse(geoCodingService: GeoCodingService,
    elevationService: ElevationService): Future[Course] = {

    val courseFeatures: Map[Int,List[HoleFeature]] =
      placemarks.foldLeft(Map[Int,List[HoleFeature]]()) { (accum,placemark) =>

        // May be getting elevation from outside world so Try it
        Try {

          val holeFeature(holeString, featureName) = placemark.name
          val hole = holeString.toInt

          val withElevation: Future[List[LatLon]] =
            Future.sequence(placemark.locations.map(elevationService.elevation(_)))

          val feature =
            withElevation.map { points =>
              HoleFeature(None, featureName, points)
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
        holes.size, Exclusivity.Public, "", courseLatLon, holes)
    }
  }

  lazy val kml: String = {

    val kmlElem =
      <kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2" xmlns:kml="http://www.opengis.net/kml/2.2" xmlns:atom="http://www.w3.org/2005/Atom">
        <Document>
          <name>{ name }.kml</name>
          <Folder>
            <name>{ name }</name>
            <open>0</open>
            { placemarks.map(placemarkElem) }
          </Folder>
        </Document>
      </kml>

    val kmlElemString = new PrettyPrinter(80, 2).format(kmlElem)

    s"""<?xml version="1.0" encoding="UTF-8"?>
      |${kmlElemString}
    """.stripMargin
  }

  private[this] def placemarkElem(placemark: KMLPlacemark) = {

    val coordinateString =
      placemark.locations.map { latLon =>
        s"${latLon.longitude},${latLon.latitude},${latLon.altitude}"
      }.mkString(" ")


    placemark.locations match {
      case head :: tail :: _ => {
        <Placemark>
          <name>{ placemark.name }</name>
          <LineString>
            <tessellate>1</tessellate>
            <coordinates>
              { coordinateString }
            </coordinates>
          </LineString>
        </Placemark>
      }
      case _ => {
        <Placemark>
          <name>{ placemark.name }</name>
          <Point>
            <gx:drawOrder>1</gx:drawOrder>
            <coordinates>{ coordinateString }</coordinates>
          </Point>
        </Placemark>
      }
    }

  }

  private[this] val holeFeature = """(\d+)-(.*?)""".r
}

object KMLMapping {

  def apply(kml: String): KMLMapping = {

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

    KMLMapping(mappingName, placemarks)
  }

  def apply(course: Course): KMLMapping = {

    val homePlacemark = KMLPlacemark("Home", List(course.location))
    val featurePlacemarks =
      for {
        hole <- course.holes
        feature <- hole.features
      } yield {
        KMLPlacemark(s"${hole.number}-${feature.name}", feature.coordinates)
      }

    KMLMapping(course.name, homePlacemark :: featurePlacemarks)
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
