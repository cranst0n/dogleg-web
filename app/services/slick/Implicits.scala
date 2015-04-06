package services.slick

import scala.slick.jdbc.{ GetResult, PositionedResult }

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import com.github.tminglei.slickpg.PgPostGISSupportUtils
import com.vividsolutions.jts.geom.{ Geometry, Point }

import Tables._

object Implicits {

  implicit class PgPositionedResult(val r: PositionedResult) {

    private[this] val jodaTzDateTimeFormatter =
      DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ")

    def nextJodaDateTime : DateTime = DateTime.parse(r.nextString, jodaTzDateTimeFormatter)

    def nextGeometry(): Geometry = {
      PgPostGISSupportUtils.fromLiteral[Geometry](r.nextString)
    }

    def nextGeometryOption(): Option[Geometry] = {
      r.nextStringOption.map(PgPostGISSupportUtils.fromLiteral[Geometry])
    }
  }

  implicit val dbUserGetResult: GetResult[DBUser] = GetResult(r =>
    DBUser(r.nextLongOption, r.nextString, r.nextString, r.nextString,
      r.nextBoolean, r.nextBoolean, r.nextJodaDateTime)
  )

  implicit val dbCourseGetResult: GetResult[DBCourse] = GetResult(r =>
    DBCourse(r.nextLongOption, r.nextString, r.nextString, r.nextString,
      r.nextString, r.nextInt, r.nextString, r.nextString,
      r.nextGeometry.asInstanceOf[Point], r.nextLongOption, r.nextBoolean)
  )

}
