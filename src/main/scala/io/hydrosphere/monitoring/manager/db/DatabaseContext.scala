package io.hydrosphere.monitoring.manager.db

import io.getquill.{Literal, PostgresZioJdbcContext}
import zio.ZIO

/** Quill Database context with as-is naming conversion and postgresql-specific features.
  */
class DatabaseContext extends PostgresZioJdbcContext(Literal)

object DatabaseContext {
  def layer = ZIO.succeed(new DatabaseContext()).toLayer
}
