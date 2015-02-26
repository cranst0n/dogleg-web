package services

import models.CrashReport

trait CrashReportDAO {

  def findById(id: Long): Option[CrashReport]

  def insert(crashReport: CrashReport): CrashReport

  def list(num: Int, offset: Int): List[CrashReport]

  def delete(id: Long): Int
}
