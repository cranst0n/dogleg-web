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
      hole.features.flatMap { feature =>
        feature.holeId.map { holeId =>
          holeFeatures.filter(_.id === feature.id).
            map(hf => (hf.name, hf.coordinates, hf.holeId)).
              update((feature.name, createMultiPoint(feature.coordinates),
                holeId))

          feature
        }
      }
    }
  }

  private[this] def holeFeature2DBHoleFeature(holeFeature: HoleFeature) = {
    DBHoleFeature(holeFeature.id, holeFeature.name,
      createMultiPoint(holeFeature.coordinates), holeFeature.holeId)
  }

  private[this] val geometryFactory = new GeometryFactory()

  private[this] def createMultiPoint(coordinates: List[LatLon]) = {
    geometryFactory.createMultiPoint(
      coordinates.map(LatLon.toVividPoint).toArray
    )
  }
}
