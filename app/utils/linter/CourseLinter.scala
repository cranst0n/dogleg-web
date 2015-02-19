package utils.linter

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.util.{ Failure, Success, Try }

import java.nio.file.{ Files, Path, Paths }

import org.apache.commons.io.FileUtils

import scaldi.Injectable

import play.api.libs.json._
import play.core.StaticApplication

import models._

import services.{ ElevationService, GeoCodingService }

object CourseKmlLinter extends App with CourseLinterApp {

  implicit val playApp = new StaticApplication(Paths.get(".").toFile)

  implicit val injector = services.ProductionModule()
  val geoCodingService = Injectable.inject[GeoCodingService]
  val elevationService = Injectable.inject[ElevationService]

  val fileTypes = Array("kml")
  val lintOp = CourseKmlLintCheck(_)
  val parseCourse = (path: Path) => {
    KMLMapping.fromKML(Source.fromFile(path.toString).getLines.mkString("\n"),
      geoCodingService, elevationService
    ).toCourse.map { course =>
      Success(course)
    }
  }

  lintApp
}

object CourseJsonLinter extends App with CourseLinterApp {

  val fileTypes = Array("json")
  val lintOp = CourseJsonLintCheck(_)
  val parseCourse = (path: Path) => {

    val jsVal =
    Json.parse(Source.fromFile(path.toString).getLines.mkString("\n"))

    Future.successful(
      jsVal.validate[models.Course] match {
        case JsSuccess(course, _) => Success(course)
        case JsError(errors) => {
          Failure(new RuntimeException(
            s"""Bad JSON File: ${path.toString}: ${errors.mkString(",")}"""))
        }
      }
    )
  }

  lintApp
}

trait CourseLinterApp { self: App =>

  def fileTypes: Array[String]

  def parseCourse: (Path => Future[Try[Course]])

  def lintOp: (Course => List[Lint])

  def lintApp = {
    args.headOption.map { lintPath =>

      val path = Paths.get(lintPath)
      val validExt =
        fileTypes.exists(ext => path.toString.toLowerCase.endsWith(ext))

      path match {
        case f if Files.isRegularFile(path) && validExt => {
          lintFile(f).map(lint => printLint(f, lint))
        }
        case dir if Files.isDirectory(path) => {
          val fileLints =
            Future.sequence(
              FileUtils.listFiles(dir.toFile, fileTypes, true).map { f =>
                lintFile(f.toPath).map(lint => printLint(f.toPath, lint))
              }
            )

          fileLints.map { lintList =>
            println(s"\n>>  Linted ${lintList.size} files with ${lintList.flatten.size} lint found.\n")
          }
        }
        case _ => exit(s"Unknown path provided: $lintPath")
      }

    }.getOrElse {
      exit("Please provide a file path to lint course JSON/KML file(s).")
    }
  }

  private[this] def lintFile(f: Path): Future[List[Lint]] = {
    parseCourse(f).map { tryRes =>
      tryRes match {
        case Success(course) => {
          lintOp(course)
        }
        case Failure(ex) => {
          List(Lint(s"Failed to parse file: ${ex.getMessage}", ""))
        }
      }
    }
  }

  private[this] def printLint(filePath: Path, lint: List[Lint]): List[Lint] = {

    lint match {
      case Nil => lint
      case l => {

        println(s"\n  >> $filePath has ${lint.size} warnings !!!!")

        lint.foreach { warn =>
          println(s"     WARNING: ${warn.message}")
          println(s"       ${warn.help}")
        }

        lint
      }
    }
  }

  private[this] def exit(message: String) {
    println(s"\n    !!!! $message !!!!\n")
    System.exit(1)
  }
}
