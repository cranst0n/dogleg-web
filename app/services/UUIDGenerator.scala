package services

import java.util.UUID

trait UUIDGenerator {
  def newUUID: UUID
}

class DefaultUUIDGenerator extends UUIDGenerator {
  override def newUUID: UUID = UUID.randomUUID()
}
