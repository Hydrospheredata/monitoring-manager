package io.hydrosphere.monitoring.manager.domain.data

import cats.kernel.instances.StringMonoid
import io.hydrosphere.monitoring.manager.domain.data.InferenceSubscriptionService.S3Data
import io.hydrosphere.monitoring.manager.domain.model.{Model, ModelRepository}
import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId
import monitoring_manager.monitoring_manager.{AnalyzedAck, DataObject}
import zio.{Has, Ref, Schedule, ZHub, ZIO, ZRef}
import zio.logging.Logger
import zio.stream.ZStream

import java.time.{Duration, Instant, LocalDateTime, OffsetDateTime}

case class InferenceSubscriptionService(
    log: Logger[String],
    modelRepository: ModelRepository,
    s3Client: S3Client,
    objIndex: S3ObjectIndex,
    hubsState: Ref[Map[PluginId, zio.Hub[S3Data]]],
    streams: Ref[Map[PluginId, ZStream[Any, Nothing, S3Data]]],
    cap: Int
) {
  def markObjSeen(
      pluginId: String,
      obj: S3Ref
  ) = objIndex.mark(pluginId, obj)

  def hubGetOrSet(pluginId: PluginId) = {
    val getHub = hubsState.get
      .flatMap(m => ZIO.fromOption(m.get(pluginId))) <* log.debug(s"Got $pluginId hub from cache")
    val createHub = for {
      hub <- ZHub.bounded[S3Data](cap)
      _   <- hubsState.update(m => m + (pluginId -> hub))
      _   <- log.debug(s"Created $pluginId hub")
    } yield hub
    getHub.orElse(createHub)
  }

  def getStream(pluginId: PluginId) = streams.get.flatMap(m => ZIO.fromOption(m.get(pluginId)))

  def subscribe(pluginId: PluginId) = {
    val streamCreate = hubGetOrSet(pluginId)
      .map(x => ZStream.fromHub(x))
      .ensuring(removeStream(pluginId))
    ZStream.fromEffect(getStream(pluginId).orElse(streamCreate)).flatten
  }

  def removeStream(pluginId: PluginId) = streams.update(streamMap => streamMap - pluginId)

  def startMonitoring =
    monitoringStep
      .tap { case (model, obj) =>
        hubsState.get.flatMap { stateMap =>
          log.info(s"Sending ${obj.fullPath} to ${stateMap.keys.toList}") *>
            ZIO
              .foreach(stateMap.toSeq) { case (pluginId, hub) =>
                log.debug(s"Got object $obj") *>
                  (hub.publish(model -> obj).whenM(objIndex.isNew(pluginId, obj)))
              }
              .unit
        }
      }
      .forever
      .schedule(Schedule.spaced(Duration.ofSeconds(10)))
      .tapError(err => log.throwable("S3 Monitoring loop failed", err))
      .ensuring(log.warn("S3 Monitoring loop finished"))

  def monitoringStep: ZStream[Has[ModelRepository], Throwable, S3Data] =
    ModelRepository
      .all()
      .map(x => x -> x.inferenceDataPrefix)
      .collect { case (m, Some(prefix)) => m -> prefix }
      .flatMap { case (m, prefix) => s3Client.getPrefixData(prefix.u).map(o => m -> o.toRef) }
      .tap(d => log.debug(s"Got from S3 $d"))
}

object InferenceSubscriptionService {
  type S3Data = (Model, S3Ref)

  def make(
      s3Client: S3Client,
      objIndex: S3ObjectIndex,
      modelRepository: ModelRepository,
      log: Logger[String]
  ): ZIO[Any, Nothing, InferenceSubscriptionService] = for {
    state   <- ZRef.make(Map.empty[PluginId, zio.Hub[S3Data]])
    streams <- ZRef.make(Map.empty[PluginId, ZStream[Any, Nothing, S3Data]])
    cap = 20 //TODO(bulat): move to config
  } yield InferenceSubscriptionService(log, modelRepository, s3Client, objIndex, state, streams, cap)

  val layer = (for {
    log             <- ZIO.service[Logger[String]]
    s3Client        <- ZIO.service[S3Client]
    objIndex        <- ZIO.service[S3ObjectIndex]
    modelRepository <- ZIO.service[ModelRepository]
    impl            <- make(s3Client, objIndex, modelRepository, log)
  } yield impl).toLayer
}
