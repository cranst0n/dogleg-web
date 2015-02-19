package services.slick

import slick.driver.PostgresDriver

import com.github.tminglei.slickpg._

trait DoglegPostgresDriver extends PostgresDriver with PgDateSupportJoda
  with PgPlayJsonSupport with PgSearchSupport with PgPostGISSupport {

  override lazy val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus {}

  trait ImplicitsPlus extends Implicits with DateTimeImplicits
    with JsonImplicits with SearchImplicits with PostGISImplicits

  trait SimpleQLPlus extends SimpleQL with ImplicitsPlus
}

object DoglegPostgresDriver extends DoglegPostgresDriver
