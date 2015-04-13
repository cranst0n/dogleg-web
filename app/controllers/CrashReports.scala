package controllers

import scaldi.Injector

import play.api.libs.json._
import play.api.mvc._

import models.CrashReport

import services.{ CrashReportDAO, MailerService, TimeSource }

class CrashReports(implicit val injector: Injector) extends DoglegController with Security {

  lazy val timeSource = inject[TimeSource]
  lazy val mailer = inject[MailerService]
  lazy val crashReportDAO = inject[CrashReportDAO]

  def submit(): Action[JsValue] = Action(parse.json) { implicit request =>

    val insertedReport =
      crashReportDAO.insert(CrashReport(None, timeSource.nowUtc, request.body))

    mailer.selfAddress.map { selfAddress =>
      mailer.sendText(selfAddress, selfAddress,
        s"""Crash Report: ${(request.body \ "STACK_TRACE").as[JsString].value.takeWhile(_ != '\n')}""",
        s"""
          |  A new crash report has been generated:
          |
          |    Report ID:       ${insertedReport.id.map(_.toString).getOrElse("N/a")}
          |    Date/Time:       ${timeSource.now.toString("MMM dd yyyy @ hh:mma")}
          |    Android Version: ${request.body \ "ANDROID_VERSION"}
          |    Dogleg Version:  ${request.body \ "BUILD_CONFIG" \ "VERSION_NAME"}
          |
          |    Stack Trace
          |  -------------------------------------------------------------------
          |
          |  ${(request.body \ "STACK_TRACE").as[JsString].value}
          |
          |""".stripMargin)
    }

    ok("Crash Report submitted.", "Sorry.")
  }

  def list(num: Int, offset: Int) = Action(parse.empty) { implicit request =>
    Ok(Json.toJson(crashReportDAO.list(1, offset)))
  }

  def byId(id: Long) = Action(parse.empty) { implicit request =>
    crashReportDAO.findById(id).map { crashReport =>
      Ok(Json.toJson(crashReport))
    } getOrElse notFound("Unknown report ID")
  }
}
