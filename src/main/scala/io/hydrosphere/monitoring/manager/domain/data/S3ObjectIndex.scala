package io.hydrosphere.monitoring.manager.domain.data

import io.hydrosphere.monitoring.manager.domain.data.S3ObjectIndex.IndexKey
import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId
import io.hydrosphere.monitoring.manager.util.{URI, ZDeadline}
import zio.clock.Clock
import zio.macros.accessible
import zio.{Ref, ZIO, ZRef}

import java.time.Instant
import scala.concurrent.duration._

@accessible
trait S3ObjectIndex {
  def isNew(pluginId: PluginId, obj: S3Ref): ZIO[Clock, Throwable, Boolean]
}

object S3ObjectIndex {
  case class IndexKey(
      pluginId: PluginId,
      s3Uri: URI
  )

  def make(): ZIO[Any, Nothing, S3ObjectIndex] = for {
    state <- ZRef.make(Map.empty[IndexKey, ZDeadline])
  } yield S3ObjectIndexImpl(state): S3ObjectIndex

  val layer = make().toLayer
}

case class S3ObjectIndexImpl(state: Ref[Map[IndexKey, ZDeadline]], deadlineAfter: FiniteDuration = 1.minute)
    extends S3ObjectIndex {
  def isNew(pluginId: PluginId, obj: S3Ref): ZIO[Clock, Throwable, Boolean] = {
    val key         = IndexKey(pluginId, obj.fullPath)
    val getDeadline = state.get.map(s => s.get(key))
    getDeadline.flatMap {
      case Some(deadline) =>
        deadline.isOverdue.flatMap {
          case true =>
            state.update(state => state - key) *>
              ZIO(true)
          case false =>
            ZIO(false)
        }
      case None =>
        for {
          now <- ZDeadline.now
          deadline = now + deadlineAfter
          _ <- state.update(s => s + (key -> deadline))
        } yield true
    }
  }
}
