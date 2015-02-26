package controllers

import scaldi.Injector

import play.api.libs.json._
import play.api.mvc._

import models.CrashReport

import services.{ CrashReportDAO, TimeSource }

class CrashReports(implicit val injector: Injector) extends DoglegController with Security {

  lazy val timeSource = inject[TimeSource]
  lazy val crashReportDAO = inject[CrashReportDAO]

  def submit(): Action[JsValue] = Action(parse.json) { implicit request =>
    crashReportDAO.insert(CrashReport(None, timeSource.nowUtc, request.body))
    ok("Crash Report submitted.", "Sorry.")
  }

  def list(num: Int, offset: Int) = Action(parse.empty) { implicit request =>
    Ok(Json.toJson(crashReportDAO.list(num, offset)))
  }
}
