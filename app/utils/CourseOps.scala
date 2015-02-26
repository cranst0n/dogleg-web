package utils

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.util.{ Failure, Success, Try }

import java.io.{ File, FilenameFilter }
import java.nio.file.Paths

import org.apache.commons.io.FileUtils._
import org.apache.commons.io.filefilter.IOFileFilter

import scaldi.{ Injectable, Injector }

import play.api.libs.json._
import play.core.StaticApplication

import models.{ Course, Gender, KMLMapping, Hole }
import Gender._

import services._
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

  private[this] val dirFilter = new IOFileFilter {
    def accept(file: File): Boolean = true
    def accept(file: File, name: String): Boolean = true
  }
}

abstract class CourseOpsApp[T] extends App with Injectable {

  def op(file: File, course: Course): T

  def filter(file: File): Boolean = {
    file.getName.toLowerCase.contains(courseNameFilter.toLowerCase) &&
      isFileExtension(file.getName, "json")
  }

  implicit val playApp = new StaticApplication(Paths.get(".").toFile)

  implicit val module = ProductionModule()
  module.bind[ElevationService] to new GoogleElevationService()

  val rootDir = new File(
    System.getProperty("user.home") + "/workspace/dogleg-courses/us/")

  val courseNameFilter = args.headOption.getOrElse("<name_of_course>")

  CourseOps.courseOp(rootDir) { filter(_) }{ (courseFile, course) =>
    op(courseFile, course)
  }
}

object AddElevation extends CourseOpsApp[Unit] {

  override def op(file: File, course: Course): Unit = {

    println(s"Adding elevation to ${file.getAbsolutePath}")

    val courseWithElevation = AddCourseElevation.toCourse(course)
    val jsonString = Json.prettyPrint(Json.toJson(courseWithElevation))

    writeToFile(file, jsonString)
  }
}

object UpdateFromKml extends CourseOpsApp[Unit] {

  override def op(file: File, course: Course): Unit = {

    val kmlFile =
      new File(file.getAbsolutePath.reverse.dropWhile(_ != '.').reverse + "kml")

    println(s"Extracting KML data from ${kmlFile.getAbsolutePath}")

    val kmlCourseHoles: Future[List[Hole]] =
      KMLMapping(Source.fromFile(kmlFile).getLines.mkString).
        toCourse(new MockGeoCodingService(), new MockElevationService()).
        map(_.holes)

    kmlCourseHoles.map { newHoles =>
      val courseWithPaths =
        AddCourseElevation.toCourse(course.copy(holes = newHoles))

      val jsonString = Json.prettyPrint(Json.toJson(courseWithPaths))

      writeToFile(file, jsonString)

      println(s"Update finished.")
    }
  }
}
