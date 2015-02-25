package controllers

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.io.Source
import scala.util.{ Failure, Success, Try }

import java.io.File

import org.apache.commons.io.IOUtils

import scaldi.Injector

import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._

import models.{ Course, KMLMapping, LatLon }

import services._

import utils.FileUtils._
import utils.linter.CourseJsonLintCheck

case class CourseApproveRequest(courseId: Long, course: Course)

object CourseApproveRequest {
  implicit val jsonFormat = Json.format[CourseApproveRequest]
}

class Courses(implicit val injector: Injector) extends DoglegController with Security {

  val importMaxSize = 10 * 1024 * 1024

  lazy val courseDAO = inject[CourseDAO]
  lazy val roundDAO = inject[RoundDAO]
  lazy val geoCodingService = inject[GeoCodingService]
  lazy val elevationService = inject[ElevationService]

  def list(lat: Option[Double], lon: Option[Double],
    num: Int, offset: Int, approved: Boolean): Action[Unit] = Action(parse.empty) { implicit request =>

    val latLon = lat.flatMap(latitude => lon.map(LatLon(latitude, _)))

    Ok(Json.toJson(courseDAO.list(latLon, num.min(CourseDAO.MaxListSize), offset, approved).map(_.summary)))
  }

  def search(searchText: String, num: Int, offset: Int): Action[Unit] = Action(parse.empty) { implicit request =>
    Ok(Json.toJson(
      courseDAO.search(searchText, num.min(CourseDAO.MaxListSize), offset).
      map(_.summary)
    ))
  }

  def info(id: Long): Action[Unit] = Action(parse.empty) { implicit request =>
    courseDAO.findById(id).map { course =>
      Ok(Json.toJson(course))
    } getOrElse notFound("Course not found", "Unknown ID")
  }

  def recentForUser: Action[Unit] = HasToken(parse.empty) { implicit request =>
    Ok(Json.toJson(
      roundDAO.list(request.user).map(_.course.summary).toSet.take(5)
    ))
  }

  def approve: Action[JsValue] = Admin(parse.json) { implicit request =>
    expect[CourseApproveRequest] { approvalRequest =>
      // First put any changes into DAO, then toggle it
      courseDAO.update(approvalRequest.course).flatMap { updatedCourse =>
        courseDAO.approve(approvalRequest.courseId).map { approvedCourse =>
          Ok(Json.toJson(approvedCourse))
        }
      } getOrElse notFound("Course not found", "Unknown ID")
    }
  }

  def delete(id: Long): Action[Unit] = Admin(parse.empty) { implicit request =>
    if(courseDAO.delete(id) > 0) {
      ok("Course deleted")
    } else {
      notFound("Course not found", "Unknown ID")
    }
  }

  def createCourse(): Action[JsValue] = HasToken(parse.json) { implicit request =>
    expect[Course] { course =>
      Ok(Json.toJson(
        courseDAO.insert(course.copy(
          creatorId = request.user.id,
          approved = Some(false)
        ))
      ))
    }
  }

  def parseFile(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    expectAsync[FileUpload] { courseFile =>
      parseCourseFile(courseFile).map { courseTry =>
        courseTry match {
          case Success(c) => Ok(Json.toJson(c))
          case Failure(ex) => badRequest(s"Parse failed: ${ex.getMessage}")
        }
      }
    }
  }

  def importFiles(): Action[JsValue] = Admin.async(parse.json(maxLength = importMaxSize)) { implicit request =>
    expectAsync[List[FileUpload]] { upload =>

      val courseFiles =
        upload match {
          case f :: Nil => {
            fileExtension(f.filename) match {
              case Some("zip") => extractZipFile(f)
              case _ => upload
            }
          }
          case _ => upload
        }

      parseCourseFiles(courseFiles).map(
        _ match {
          case Success(courses) => ok(s"Imported ${courses.size} courses.")
          case Failure(ex) => badRequest(s"Import failed: ${ex.getMessage}")
        }
      )
    }
  }

  def importZip(): Action[JsValue] = Admin.async(parse.json(maxLength = importMaxSize)) { implicit request =>
    expectAsync[FileUpload] { zipFile =>
      parseCourseFiles(extractZipFile(zipFile)).map(
        _ match {
          case Success(courses) => ok(s"Imported ${courses.size} courses.")
          case Failure(ex) => badRequest(s"Import failed: ${ex.getMessage}")
        }
      )
    }
  }

  def lint(): Action[JsValue] = HasToken(parse.json) { implicit request =>
    expect[Course] { course =>
      Ok(Json.toJson(CourseJsonLintCheck(course)))
    }
  }

  def raw(id: Long): Action[Unit] = Action(parse.empty) { implicit request =>
    courseDAO.findById(id).map { course =>
      Ok(Json.toJson(course.raw))
    } getOrElse notFound("Course not found", "Unknown ID")
  }

  private[this] def parseCourseFiles(courseFiles: List[FileUpload])(implicit request: TokenRequest[JsValue]): Future[Try[List[Course]]] = {
    Future.sequence(courseFiles.map(parseCourseFile)).map { list =>
      list.filter(_.isFailure) match {
        case Nil => {  // No failures so put them in the DB
          Try {
            list.map(_.get).map { course =>
              courseDAO.insert(course.copy(
                creatorId = request.user.id,
                approved = Some(true)
              ))
            }
          }
        }
        case errors => {
          Failure(new RuntimeException(errors.mkString))
        }
      }
    }
  }

  private[this] def parseCourseFile(courseFile: FileUpload): Future[Try[Course]] = {
    (fileExtension(courseFile.filename) match {
      case Some("kml") => {
        KMLMapping.fromKML(courseFile.content, geoCodingService,
          elevationService).toCourse
      }
      case Some("json") => {
        Json.parse(courseFile.content).validate[Course] match {
          case JsSuccess(course,_) => Future.successful(course)
          case JsError(errors) => {
            Future.failed(new IllegalArgumentException(
              s"Invalid JSON: ${courseFile.filename}"))
          }
        }
      }
      case Some(ext) => {
        Future.failed(new IllegalArgumentException(s"Invalid file type: $ext"))
      }
      case _ => {
        Future.failed(new IllegalArgumentException(
          s"Unknown file type: ${courseFile.filename}"))
      }
    }).map(Success(_)).recover { case ex => Failure(ex) }
  }

  private[this] def extractZipFile(fileUpload: FileUpload): List[FileUpload] = {

    val tmpFile = File.createTempFile("tmp", fileUpload.filename)
    writeToFile(tmpFile, fileUpload.bytes)
    val zipFile = new java.util.zip.ZipFile(tmpFile)

    val files =
      zipFile.entries.asScala.map { entry =>
        using(zipFile.getInputStream(entry)) { entryStream =>
          FileUpload(entry.getSize, fileExtension(entry.getName).getOrElse(""),
            entry.getName, IOUtils.toString(entryStream))
        }
      }

    tmpFile.delete

    files.toList
  }
}
