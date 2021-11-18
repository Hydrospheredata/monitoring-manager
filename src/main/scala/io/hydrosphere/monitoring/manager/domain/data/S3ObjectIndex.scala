package io.hydrosphere.monitoring.manager.domain.data

import zio.{Ref, ZIO, ZRef}
import zio.logging.log

object S3ObjectIndex {
  def make(): ZIO[Any, Nothing, S3ObjectIndex] = for {
    state <- ZRef.make(Set.empty[(String, S3Ref)])
  } yield S3ObjectIndex(state)

  val layer = make().toLayer
}

case class S3ObjectIndex(state: Ref[Set[(String, S3Ref)]]) {
  def isNew(pluginId: String, obj: S3Ref) = {
    val objSeen = state.get.map(s => s(pluginId -> obj))
    objSeen.tap {
      case true =>
        log.debug(s"$pluginId saw $obj").as(false)
      case false => log.debug(s"$pluginId didn't see $obj")
    }
  }

  def mark(pluginId: String, obj: S3Ref) =
    state.updateAndGet(s => s + (pluginId -> obj)).unit
}
