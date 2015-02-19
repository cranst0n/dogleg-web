package models

import play.api.libs.json.Json

case class Hole(id: Option[Long], number: Int, courseId: Option[Long] = None,
  features: List[HoleFeature] = Nil) {

  lazy val estimateYardage: Int = {

    def impl(features: List[HoleFeature], totalYardage: Double): Double = {
      features match {
        case Nil => totalYardage
        case head :: Nil => totalYardage
        case head :: second :: tail => {
          head.estimateCenter.distance(second.estimateCenter) +
            impl(second :: tail,totalYardage)
        }
      }
    }


    impl(features,0).toInt
  }

  lazy val estimatePar: Int = {
    estimateYardage match {
      case 0 => 0
      case x if x <= Hole.Par3MaxDistance => 3
      case x if x <= Hole.Par4MaxDistance => 4
      case x if x <= Hole.Par5MaxDistance => 5
      case _ => 6
    }
  }

  def featureForName(name: String): Option[HoleFeature] = {
    features.find(_.name.toLowerCase == name)
  }
}

object Hole {
  implicit val format = Json.format[Hole]

  val Par3MaxDistance = 250
  val Par4MaxDistance = 470
  val Par5MaxDistance = 650
}
