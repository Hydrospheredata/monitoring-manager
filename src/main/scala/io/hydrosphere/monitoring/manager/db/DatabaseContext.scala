package io.hydrosphere.monitoring.manager.db

import io.getquill.{Literal, PostgresZioJdbcContext}
import zio.ZIO

import java.time.{Instant, LocalDateTime, ZoneId}

/** Quill Database context with as-is naming conversion and postgresql-specific features.
  */
class DatabaseContext extends PostgresZioJdbcContext(Literal) {
  implicit val instantEncoder: MappedEncoding[Instant, LocalDateTime] =
    MappedEncoding[Instant, LocalDateTime](LocalDateTime.ofInstant(_, ZoneId.systemDefault()))
  implicit val instantDecoder: MappedEncoding[LocalDateTime, Instant] =
    MappedEncoding[LocalDateTime, Instant](_.atZone(ZoneId.systemDefault()).toInstant)
}

object DatabaseContext {
  val layer = ZIO.succeed(new DatabaseContext()).toLayer
}
