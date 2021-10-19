package io.hydrosphere.monitoring.manager.domain.data

import io.github.vigoo.zioaws.s3.model.S3Object
import zio.{Ref, ZIO, ZRef}
import zio.logging.log

object S3ObjectIndex {
  def make(): ZIO[Any, Nothing, S3ObjectIndex] = for {
    state <- ZRef.make(Set.empty[(String, S3Object)])
  } yield S3ObjectIndex(state)

  val layer = make().toLayer
}

case class S3ObjectIndex(state: Ref[Set[(String, S3Object)]]) {
  def isNew(pluginId: String, obj: S3Object.ReadOnly) = {
    val objSeen = state.get.map(s => s(pluginId -> obj.editable))
    val objAdded =
      state
        .updateAndGet(s => s + (pluginId -> obj.editable))
    objSeen.flatMap {
      case true =>
        log.info(s"$pluginId saw ${obj.editable}").as(false)
      case false => log.info(s"$pluginId didn't see ${obj.editable}") *> objAdded.as(true)
    }
  }
}
