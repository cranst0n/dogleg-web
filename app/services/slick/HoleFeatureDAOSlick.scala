package services.slick

import scaldi.{ Injectable, Injector }

import com.vividsolutions.jts.geom.{ Coordinate, GeometryFactory, Point }

import play.api.Play.current
import play.api.db.slick._

import DoglegPostgresDriver.simple._
import Tables._

import models.{ Hole, HoleFeature, LatLon }

import services.HoleFeatureDAO

class HoleFeatureDAOSlick(implicit val injector: Injector)
  extends HoleFeatureDAO with Injectable {

  override def forHole(holeId: Long): List[HoleFeature] = {
    DB withSession { implicit session =>
      holeFeatures.filter(_.holeId === holeId).list.map(_.toHoleFeature)
    }
  }

  override def insert(hole: Hole): List[HoleFeature] = {
    DB withSession { implicit session =>
      ((holeFeatures returning holeFeatures) ++=
        hole.features.map(holeFeature2DBHoleFeature).
          map(_.copy(holeId = hole.id))
      ).toList.map(_.toHoleFeature)
    }
  }

  override def update(hole: Hole): List[HoleFeature] = {
    DB withSession { implicit session =>
      holeFeatures.filter(_.holeId === hole.id).delete
      insert(hole)
    }
  }

  private[this] val geometryFactory = new GeometryFactory()

  private[this] def holeFeature2DBHoleFeature(holeFeature: HoleFeature) = {
    val multiPoint = geometryFactory.createMultiPoint(
      holeFeature.coordinates.map(LatLon.toVividPoint).toArray
    )

    DBHoleFeature(holeFeature.name, multiPoint, holeFeature.holeId)
  }
}
