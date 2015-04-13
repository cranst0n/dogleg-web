package services

import scala.concurrent.duration._
import scala.concurrent.duration.Duration._

import akka.actor.Cancellable

import play.api.Play
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.api.libs.mailer.{ Email, MailerPlugin }

import com.typesafe.plugin._

trait MailerService {

  def sendText(recipient: String, from: String,
    subject: String, text: String): Cancellable = {

    Akka.system.scheduler.scheduleOnce(1.seconds) {
      sendText(List(recipient),Nil,from,subject,text)
    }
  }

  def sendHtml(recipient: String, from: String,
    subject: String, html: String): Cancellable = {

    Akka.system.scheduler.scheduleOnce(1.seconds) {
      sendHtml(List(recipient),Nil,from,subject,html)
    }
  }

  def sendText(recipients: List[String], bcc: List[String], from: String,
    subject: String, text: String): String

  def sendHtml(recipients: List[String], bcc: List[String], from: String,
    subject: String, html: String): String

  def selfAddress: Option[String] = {
    Play.current.configuration.getString("smtp.user")
  }

}

object MailerService {

  lazy val PlayMailer: MailerService = {

    new MailerService {

      override def sendText(recipients: List[String], bcc: List[String],
        from: String, subject: String, text: String) = {

        MailerPlugin.send(
          Email(subject, from, recipients, bcc = bcc, bodyText = Some(text))
        )
      }

      override def sendHtml(recipients: List[String], bcc: List[String],
        from: String, subject: String, html: String) = {
        MailerPlugin.send(
          Email(subject, from, recipients, bcc = bcc, bodyHtml = Some(html))
        )
      }
    }
  }

}
