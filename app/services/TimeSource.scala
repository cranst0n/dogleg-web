package services

import org.joda.time.{ DateTime,DateTimeZone }

trait TimeSource {

  def now: DateTime

  def nowUtc: DateTime = now.withZone(DateTimeZone.UTC)

  def currentTimeMillis: Long = nowUtc.getMillis
}

object TimeSource {
  def system: TimeSource = new TimeSource {
    override def now: DateTime = new DateTime
    override def currentTimeMillis: Long = System.currentTimeMillis
  }
}
