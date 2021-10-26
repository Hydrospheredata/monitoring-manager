package io.hydrosphere.monitoring.manager.domain.data

import zio.{Ref, ZIO, ZRef}
import zio.logging.log

object S3ObjectIndex {
  def make(): ZIO[Any, Nothing, S3ObjectIndex] = for {
    state <- ZRef.make(Set.empty[(String, S3Obj)])
  } yield S3ObjectIndex(state)

  val layer = make().toLayer
}

case class S3ObjectIndex(state: Ref[Set[(String, S3Obj)]]) {
  def isNew(pluginId: String, obj: S3Obj) = {
    val objSeen = state.get.map(s => s(pluginId -> obj))
    val objAdded =
      state
        .updateAndGet(s => s + (pluginId -> obj))
    objSeen.flatMap {
      case true =>
        log.info(s"$pluginId saw $obj").as(false)
      case false => log.info(s"$pluginId didn't see $obj") *> objAdded.as(true)
    }
  }
}
