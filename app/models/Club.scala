package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

sealed trait Club {
  def id: Int
  def name: String

  protected val Degree = "\u00b0";
}

object Club {

  implicit val clubReads: Reads[Club] = (
    (JsPath \ "id").read[Int] and
    (JsPath \ "name").read[String]
  ) { (jsonId, jsonName) =>
    new Club {
      override val id = jsonId
      override val name = jsonName
    }
  }

  implicit val clubWrites = new Writes[Club] {
    override def writes(club: Club) = {
      Json.obj(
        "id" -> club.id,
        "name" -> club.name
      )
    }
  }

  def forId(id: Int) = {
    id match {
      case Driver.id => Driver
      case Wood2.id => Wood2
      case Wood3.id => Wood3
      case Wood4.id => Wood4
      case Wood5.id => Wood5
      case Wood7.id => Wood7
      case Wood9.id => Wood9
      case Wood11.id => Wood11
      case Hybrid1.id => Hybrid1
      case Hybrid2.id => Hybrid2
      case Hybrid3.id => Hybrid3
      case Hybrid4.id => Hybrid4
      case Hybrid5.id => Hybrid5
      case Hybrid6.id => Hybrid6
      case Hybrid7.id => Hybrid7
      case Hybrid8.id => Hybrid8
      case Hybrid9.id => Hybrid9
      case Iron1.id => Iron1
      case Iron2.id => Iron2
      case Iron3.id => Iron3
      case Iron4.id => Iron4
      case Iron5.id => Iron5
      case Iron6.id => Iron6
      case Iron7.id => Iron7
      case Iron8.id => Iron8
      case Iron9.id => Iron9
      case WedgeP.id => WedgeP
      case Wedge50.id => Wedge50
      case Wedge52.id => Wedge52
      case Wedge54.id => Wedge54
      case Wedge56.id => Wedge56
      case Wedge58.id => Wedge58
      case Wedge60.id => Wedge60
      case Wedge62.id => Wedge62
      case Wedge64.id => Wedge64
      case Putter.id => Putter
      case _ => Unknown
    }
  }
}

case object Driver extends Club { override val id = 0; override val name = "Driver" }

case object Wood2 extends Club {  override val id = 102; override val name = "2 Wood" }
case object Wood3 extends Club {  override val id = 103; override val name = "3 Wood" }
case object Wood4 extends Club {  override val id = 104; override val name = "4 Wood" }
case object Wood5 extends Club {  override val id = 105; override val name = "5 Wood" }
case object Wood7 extends Club {  override val id = 107; override val name = "7 Wood" }
case object Wood9 extends Club {  override val id = 109; override val name = "9 Wood" }
case object Wood11 extends Club {  override val id = 111; override val name = "11 Wood" }

case object Hybrid1 extends Club {  override val id = 201; override val name = "1 Hybrid" }
case object Hybrid2 extends Club {  override val id = 202; override val name = "2 Hybrid" }
case object Hybrid3 extends Club {  override val id = 203; override val name = "3 Hybrid" }
case object Hybrid4 extends Club {  override val id = 204; override val name = "4 Hybrid" }
case object Hybrid5 extends Club {  override val id = 205; override val name = "5 Hybrid" }
case object Hybrid6 extends Club {  override val id = 206; override val name = "6 Hybrid" }
case object Hybrid7 extends Club {  override val id = 207; override val name = "7 Hybrid" }
case object Hybrid8 extends Club {  override val id = 208; override val name = "8 Hybrid" }
case object Hybrid9 extends Club {  override val id = 209; override val name = "9 Hybrid" }

case object Iron1 extends Club {  override val id = 301; override val name = "1 Iron" }
case object Iron2 extends Club {  override val id = 302; override val name = "2 Iron" }
case object Iron3 extends Club {  override val id = 303; override val name = "3 Iron" }
case object Iron4 extends Club {  override val id = 304; override val name = "4 Iron" }
case object Iron5 extends Club {  override val id = 305; override val name = "5 Iron" }
case object Iron6 extends Club {  override val id = 306; override val name = "6 Iron" }
case object Iron7 extends Club {  override val id = 307; override val name = "7 Iron" }
case object Iron8 extends Club {  override val id = 308; override val name = "8 Iron" }
case object Iron9 extends Club {  override val id = 309; override val name = "9 Iron" }

case object WedgeP extends Club {  override val id = 445; override val name = s"Pitching Wedge" }
case object Wedge50 extends Club {  override val id = 450; override val name = s"50$Degree Wedge" }
case object Wedge52 extends Club {  override val id = 452; override val name = s"52$Degree Wedge" }
case object Wedge54 extends Club {  override val id = 454; override val name = s"54$Degree Wedge" }
case object Wedge56 extends Club {  override val id = 456; override val name = s"56$Degree Wedge" }
case object Wedge58 extends Club {  override val id = 458; override val name = s"58$Degree Wedge" }
case object Wedge60 extends Club {  override val id = 460; override val name = s"60$Degree Wedge" }
case object Wedge62 extends Club {  override val id = 462; override val name = s"62$Degree Wedge" }
case object Wedge64 extends Club {  override val id = 464; override val name = s"64$Degree Wedge" }

case object Putter extends Club {  override val id = 500; override val name = "Putter" }

case object Unknown extends Club {  override val id = 1000; override val name = "Unknown" }

case object FinishHole extends Club {  override val id = 2000; override val name = "Finish Hole" }
