package services.slick

import scala.slick.jdbc.{ GetResult, PositionedResult }

import com.github.tminglei.slickpg.PgPostGISSupportUtils
import com.vividsolutions.jts.geom.{ Geometry, Point }

import Tables._

object Implicits {

  implicit class PgPositionedResult(val r: PositionedResult) {
    def nextGeometry(): Geometry = {
      PgPostGISSupportUtils.fromLiteral[Geometry](r.nextString)
    }

    def nextGeometryOption(): Option[Geometry] = {
      r.nextStringOption.map(PgPostGISSupportUtils.fromLiteral[Geometry])
    }
  }

  implicit val dbCourseGetResult: GetResult[DBCourse] = GetResult(r =>
    DBCourse(r.nextLongOption, r.nextString, r.nextString, r.nextString,
      r.nextString, r.nextInt, r.nextString, r.nextString,
      r.nextGeometry.asInstanceOf[Point], r.nextLongOption, r.nextBoolean)
  )

}
