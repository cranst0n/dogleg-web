package models

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, Node, PrettyPrinter, XML }

import scaldi.{ Injectable, Injector }

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._

import models.Exclusivity._

import services.{ ElevationService, GeoCodingService }

case class KMLPlacemark(name: String, locations: List[LatLon],
  extendedData: Map[String, String] = Map())

case class KMLMapping(name: String, placemarks: List[KMLPlacemark],
  originalCourse: Option[Course] = None,
  courseRatings: Option[List[CourseRating]] = None) {

  def toCourse(geoCodingService: GeoCodingService,
    elevationService: ElevationService): Future[Course] = {

    originalCourse match {
      case Some(course) => Future.successful(course)
      case None => {
        val courseFeatures: Map[Int,List[HoleFeature]] =
          placemarks.foldLeft(Map[Int,List[HoleFeature]]()) { (accum,placemark) =>

            // May be getting elevation from outside world so Try it
            Try {

              val holeFeature(holeString, featureName) = placemark.name
              val holeNumber = holeString.toInt

              val withElevation: Future[List[LatLon]] =
                Future.sequence(
                  placemark.locations.map(elevationService.elevation(_)))

              val feature =
                withElevation.map { points =>
                  val featureId = placemark.extendedData.get("id").map(_.toLong)
                  val holeId = placemark.extendedData.get("holeId").map(_.toLong)
                  HoleFeature(featureId, featureName, points, holeId)
                }

              accum + ((holeNumber, Await.result(feature,5.seconds) :: (accum.getOrElse(holeNumber,List()))))

            } match {
              case Success(result) => result
              case _ => accum
            }
          }

        val courseLatLon =
          homePlacemark.map(_.locations.head).
            getOrElse(courseFeatures.head._2.head.coordinates.head)

        val geoCodeLookup = geoCodingService.reverseGeoCode(courseLatLon)

        val holes =
          courseFeatures.keys.toList.map { holeNumber =>

            val holeId: Option[Long] =
              for {
                tee <- placemarks.find(_.name == s"$holeNumber-Tee")
                data <- tee.extendedData.find { case (k,v) => k == "holeId"}
              } yield {
                data._2.toLong
              }

            Hole(holeId, holeNumber, courseId, courseFeatures(holeNumber))

          }.sorted

        val ratings =
          courseRatings orElse originalCourse.map(_.ratings) getOrElse Nil

        geoCodeLookup.map { geoCode =>
          Course(courseId, name, courseCity.getOrElse(geoCode.city),
            courseState.getOrElse(geoCode.state),
            courseCountry.getOrElse(geoCode.country),
            holes.size, courseExclusivity, coursePhoneNumber, courseLatLon,
            holes, ratings, courseCreatorId, courseApproved)
        }
      }
    }
  }

  lazy val homePlacemark = placemarks.find(_.name.toLowerCase == "home")
  lazy val courseId = homePlacemark.flatMap(_.extendedData.get("id").map(_.toLong)).filter(_ > 0)
  lazy val courseCity = homePlacemark.flatMap(_.extendedData.get("city"))
  lazy val courseState = homePlacemark.flatMap(_.extendedData.get("state"))
  lazy val courseCountry = homePlacemark.flatMap(_.extendedData.get("country"))
  lazy val courseExclusivity = homePlacemark.flatMap(_.extendedData.get("exclusivity").
    map(Exclusivity.parse)).flatten.getOrElse(Exclusivity.Public)
  lazy val coursePhoneNumber = homePlacemark.flatMap(_.extendedData.get("phoneNumber")).
    getOrElse("")
  lazy val courseCreatorId = homePlacemark.flatMap(_.extendedData.get("creatorId").map(_.toLong)).filter(_ > 0)
  lazy val courseApproved = homePlacemark.flatMap(_.extendedData.get("approved").map(_.toBoolean))

  lazy val kml: String = {

    val kmlElem =
      <kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2" xmlns:kml="http://www.opengis.net/kml/2.2" xmlns:atom="http://www.w3.org/2005/Atom">
        <Document>
          <name>{ name }.kml</name>
          <Folder>
            <name>{ name }</name>
            <open>0</open>
            { placemarks.map(placemarkElem) }
            {
              originalCourse match {
                case Some(course) => {
                  <ratings>
                    { Json.stringify(Json.toJson(course.ratings)) }
                  </ratings>
                }
                case None =>
              }
            }
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
      case head :: second :: _ => {
        <Placemark>
          <name>{ placemark.name }</name>
          { placemarkExtendedDataElem(placemark) }
          <LineString>
            <coordinates>
              { coordinateString }
            </coordinates>
          </LineString>
        </Placemark>
      }
      case _ => {
        <Placemark>
          <name>{ placemark.name }</name>
          { placemarkExtendedDataElem(placemark) }
          <Point>
            <coordinates>{ coordinateString }</coordinates>
          </Point>
        </Placemark>
      }
    }
  }

  private def placemarkExtendedDataElem(placemark: KMLPlacemark) = {
    placemark.extendedData match {
      case m if m.nonEmpty => {
        <ExtendedData>
          {
            m.map { case (key, value) =>
              <Data name={key}>
                <value>{ value }</value>
              </Data>
            }
          }
        </ExtendedData>
      }
      case _ =>
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
        KMLPlacemark(name, parseCoordinateString(latLonString),
          parseExtendedData(placemarkElem))
      }

    val courseRatings =
      (folderElem \ "ratings").headOption.map { ratingsElem =>
        Json.parse(ratingsElem.text).validate[List[CourseRating]] match {
          case JsSuccess(ratings, _) => ratings
          case JsError(_) => Nil
        }
      }

    KMLMapping(mappingName, placemarks, None, courseRatings)
  }

  def apply(course: Course): KMLMapping = {

    val homePlacemark = KMLPlacemark("Home", List(course.location),
      Map(
        "id" -> course.id.map(_.toString),
        "city" -> Some(course.city),
        "state" -> Some(course.state),
        "country" -> Some(course.country),
        "exclusivity" -> Some(course.exclusivity.toString),
        "phoneNumber" -> Some(course.phoneNumber),
        "creatorId" -> course.creatorId.map(_.toString),
        "approved" -> course.approved.map(_.toString)
      ).collect { case (key, Some(value)) => key -> value }
    )

    val featurePlacemarks =
      for {
        hole <- course.holes
        feature <- hole.features
      } yield {
        KMLPlacemark(s"${hole.number}-${feature.name}", feature.coordinates,
          Map(
            "id" -> feature.id.map(_.toString),
            "holeId" -> hole.id.map(_.toString)
          ).collect { case (key, Some(value)) => key -> value }
        )
      }

    KMLMapping(course.name, homePlacemark :: featurePlacemarks, Some(course))
  }

  private[this] def parseCoordinateString(s: String): List[LatLon] = {
    val coordinates =
      s.trim.split(' ').map(_.split(',')).flatten.toList.map(_.toDouble)

    coordinates.sliding(3,3).toList.map {
      case lon :: lat :: alt :: Nil => Some(LatLon(lat,lon,alt))
      case _ => None
    }.flatten
  }

  private[this] def parseExtendedData(node: Node): Map[String, String] = {

    val kvPairs =
      for {
        dataNode <- node \ "ExtendedData" \\ "Data"
      } yield {
        val key = (dataNode \ "@name").text
        val value = (dataNode \ "value").text
        (key -> value)
      }

    Map(kvPairs: _*)
  }
}
