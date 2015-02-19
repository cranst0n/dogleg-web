package utils

import scala.collection.JavaConversions._
import scala.io.Source
import scala.util.{ Failure, Success, Try }

import java.io.{ File, FilenameFilter }
import java.nio.file.Paths

import org.apache.commons.io.FileUtils._
import org.apache.commons.io.filefilter.IOFileFilter

import play.api.libs.json._
import play.core.StaticApplication

import models.{ Course, Gender }
import Gender._

import services.{ ElevationService, ProductionModule }
import services.google.GoogleElevationService

import utils.FileUtils._

object CourseOps {

  def courseOp[T](rootDir: File)(filter: File => Boolean)
    (op: (File, Course) => T): List[Try[T]] = {

    val ioFilter = new IOFileFilter {
      def accept(file: File): Boolean = filter(file)
      def accept(file: File, name: String): Boolean = filter(file)
    }

    listFiles(rootDir, ioFilter, dirFilter).toList.map { courseFile =>

      val json =
        Json.parse(Source.fromFile(courseFile).getLines.mkString("\n"))

      json.validate[Course] match {
        case JsSuccess(course,_) => Success(op(courseFile, course))
        case JsError(errors) => Failure(new RuntimeException(errors.mkString))
      }
    }
  }

  private val dirFilter = new IOFileFilter {
    def accept(file: File): Boolean = true
    def accept(file: File, name: String): Boolean = true
  }
}

object AddElevation extends App {

  implicit val playApp = new StaticApplication(Paths.get(".").toFile)

  implicit val module = ProductionModule()
  module.bind[ElevationService] to new GoogleElevationService()

  val rootDir = new File(
    System.getProperty("user.home") + "/workspace/dogleg-courses/us/")

  val courseNameFilter = args.headOption.getOrElse("<name_of_course>")

  CourseOps.courseOp(rootDir) { file =>

    file.getName.toLowerCase.contains(courseNameFilter) &&
      isFileExtension(file.getName, "json")
    }{ (courseFile, course) =>

      println(s"Adding elevation to ${courseFile.getAbsolutePath}")

      val courseWithElevation = AddCourseElevation.toCourse(course)
      val jsonString = Json.prettyPrint(Json.toJson(courseWithElevation))

      writeToFile(courseFile, jsonString)
    }
}
