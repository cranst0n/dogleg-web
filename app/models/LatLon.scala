package models

import scala.language.implicitConversions

import math._

import com.vividsolutions.jts.geom.{ Coordinate, GeometryFactory, Point }

import play.api.libs.json.Json

case class LatLon(latitude: Double, longitude: Double, altitude: Double = 0.0) {

  def distance(ll: LatLon): Double = {
    distanceKm(ll) * LatLon.km2Yards
  }

  def distanceKm(ll: LatLon): Double = {
    haversine(latitude, longitude, ll.latitude, ll.longitude)
  }

  def haversine(lat1: Double, lon1: Double,
    lat2: Double, lon2: Double): Double = {

    val dLat = (lat2 - lat1).toRadians
    val dLon = (lon2 - lon1).toRadians
    val a = pow(sin(dLat/2),2) + pow(sin(dLon/2),2) *
      cos(lat1.toRadians) * cos(lat2.toRadians)
    val c = 2 * asin(sqrt(a))
    LatLon.earthRadiusKm * c
   }
}

object LatLon {

  val MinLat = -90
  val MaxLat = 90
  val MinLon = -180
  val MaxLon = 180

  protected val earthRadiusKm = 6372.8;
  protected val km2Yards = 1093.61;

  implicit val latLonFormat = Json.format[LatLon]

  private[this] val geometryFactory = new GeometryFactory()

  implicit def fromVividPoint(point: Point): LatLon = {
    fromVividCoordinate(point.getCoordinate)
  }

  implicit def fromVividPointOpt(point: Option[Point]): Option[LatLon] = {
    point.map(fromVividPoint)
  }

  implicit def fromVividCoordinate(coordinate: Coordinate): LatLon = {
    LatLon(coordinate.x, coordinate.y, coordinate.z)
  }

  implicit def toVividCoordinate(latLon: LatLon): Coordinate = {
    new Coordinate(latLon.latitude, latLon.longitude, latLon.altitude)
  }

  implicit def toVividPoint(latLon: LatLon): Point = {
    geometryFactory.createPoint(toVividCoordinate(latLon))
  }

  implicit def toVividPointOpt(latLon: Option[LatLon]): Option[Point] = {
    latLon.map(toVividPoint)
  }
}
