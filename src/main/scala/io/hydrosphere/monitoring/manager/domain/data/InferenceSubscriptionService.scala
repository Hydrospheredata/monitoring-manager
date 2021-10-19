package io.hydrosphere.monitoring.manager.domain.data

import io.github.vigoo.zioaws.s3
import io.github.vigoo.zioaws.s3.model.S3Object
import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId
import sttp.model.Uri
import zio.{Ref, Schedule, ZHub, ZIO, ZRef}
import zio.stream.{ZSink, ZStream}
import zio.logging.log

import java.time.Duration

case class InferenceSubscriptionService(
    s3Client: S3Client,
    objIndex: S3ObjectIndex,
    state: Ref[Map[PluginId, zio.Hub[s3.model.S3Object.ReadOnly]]],
    cap: Int
) {
  def subscribe(pluginId: PluginId, path: Uri) = {
    val get = state.get
      .flatMap(m => ZIO.fromOption(m.get(pluginId))) <* log.debug(s"Got $pluginId hub from cache")
    val create = for {
      hub <- ZHub.bounded[s3.model.S3Object.ReadOnly](cap)
      _   <- state.update(m => m + (pluginId -> hub))
      _   <- log.debug(s"Created $pluginId hub")
    } yield hub
    val f = for {
      hub    <- get.orElse(create)
      _      <- log.debug("Hub acquired")
      stream <- ZIO.apply(ZStream.fromHub(hub))
      _      <- log.debug("Subscription stream from hub")
      fbr <- startMonitoring(pluginId, path, hub)
        .tapError(err => log.throwable("Error while fetching objects from S3", err))
        .fork
      _ <- log.debug(s"Data collection started. Fiber ${fbr.id}")
    } yield stream.ensuring(
      fbr.interrupt
        .tap(exit => log.debug(s"Data collection fiber ${fbr.id} interrupted. Reason: $exit"))
    ) //NB(bulat): close the monitoring fiber after the stream is ended
    ZStream.fromEffect(f).flatten
  }

  def startMonitoring(pluginId: PluginId, prefix: Uri, hub: zio.Hub[S3Object.ReadOnly]) =
    publishPrefixData(pluginId, prefix, hub).repeat(Schedule.spaced(Duration.ofSeconds(10)))

  def publishPrefixData(pluginId: PluginId, prefix: Uri, hub: zio.Hub[S3Object.ReadOnly]) =
    s3Client
      .getPrefixData(prefix)
      .filterM(obj => objIndex.isNew(pluginId, obj))
      .run(ZSink.fromHub(hub))
}

object InferenceSubscriptionService {
  def make(
      s3Client: S3Client,
      objIndex: S3ObjectIndex
  ): ZIO[Any, Nothing, InferenceSubscriptionService] = for {
    state <- ZRef.make(Map.empty[PluginId, zio.Hub[s3.model.S3Object.ReadOnly]])
    cap = 20 //TODO(bulat): move to config
  } yield InferenceSubscriptionService(s3Client, objIndex, state, cap)

  val layer = (for {
    s3Client <- ZIO.service[S3Client]
    objIndex <- ZIO.service[S3ObjectIndex]
    impl     <- make(s3Client, objIndex)
  } yield impl).toLayer
}
