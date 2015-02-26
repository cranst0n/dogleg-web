package services.slick

import scaldi.{ Injectable, Injector }

import play.api.Play.current
import play.api.db.slick._

import DoglegPostgresDriver.simple._
import Tables._

import models.CrashReport

import services.CrashReportDAO

class CrashReportDAOSlick(implicit val injector: Injector)
  extends CrashReportDAO with Injectable {

  override def findById(id: Long): Option[CrashReport] = {
    DB withSession { implicit session =>
      crashReports.filter(_.id === id).firstOption
    }
  }

  override def insert(crashReport: CrashReport): CrashReport = {
    DB withSession { implicit session =>
      crashReports returning crashReports.map(_.id) into ((report, assignedId) =>
        crashReport.copy(id = Some(assignedId))
      ) += crashReport
    }
  }

  override def list(num: Int, offset: Int): List[CrashReport] = {
    DB withSession { implicit session =>
      crashReports.drop(offset).take(num).list
    }
  }

  override def delete(id: Long): Int = {
    DB withSession { implicit session =>
      crashReports.filter(_.id === id).delete
    }
  }

}
