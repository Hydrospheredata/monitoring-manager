package io.hydrosphere.monitoring.manager.domain.data

import io.hydrosphere.monitoring.manager.domain.data.InferenceSubscriptionService.S3Data
import io.hydrosphere.monitoring.manager.domain.model.{Model, ModelRepository}
import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId
import io.hydrosphere.monitoring.manager.domain.report.ReportRepository
import zio.clock.Clock
import zio.logging.Logger
import zio.stream.ZStream
import zio.{Has, Ref, Schedule, ZHub, ZIO, ZRef}

import java.time.Duration

case class InferenceSubscriptionService(
    log: Logger[String],
    modelRepository: ModelRepository,
    s3Client: S3Client,
    objIndex: S3ObjectIndex,
    hubsState: Ref[Map[PluginId, zio.Hub[S3Data]]],
    cap: Int
) {

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

  def subscribe(pluginId: PluginId) =
    ZStream.fromEffect(hubGetOrSet(pluginId)).flatMap(x => ZStream.fromHub(x))

  def startMonitoring =
    monitoringStep
      .schedule(Schedule.spaced(Duration.ofSeconds(10)))
      .forever
      .tap { case (model, obj) =>
        log.debug("Got the subscription state") *>
          hubsState.get.flatMap { stateMap =>
            ZIO
              .foreachPar_(stateMap.toSeq) { case (pluginId, hub) =>
                ((hub.publish(model -> obj) *> log.info(s"Discovered ${obj.fullPath} for $pluginId"))
                  .whenM(shouldSend(pluginId, obj)))
              }
              .unit
          }
      }
      .tapError(err => log.throwable("S3 Monitoring loop failed", err))
      .ensuring(log.warn("S3 Monitoring loop finished"))

  def shouldSend(
      pluginId: PluginId,
      obj: S3Ref
  ): ZIO[Clock with Has[ReportRepository], Throwable, Boolean] =
    ReportRepository.exists(pluginId, obj).flatMap {
      case true =>
        log.debug(s"$pluginId has a report for $obj. Won't send.") *>
          ZIO(false)
      case false =>
        objIndex.isNew(pluginId, obj).tap(isNew => log.debug(s"Index: $obj -> $pluginId? $isNew"))
    }

  def monitoringStep: ZStream[Has[ModelRepository], Throwable, S3Data] =
    ModelRepository
      .all()
      .tap(m => log.debug(s"Discovering ${m.name}:${m.version} model"))
      .map(x => x -> x.inferenceDataPrefix)
      .collect { case (m, Some(prefix)) => m -> prefix }
      .flatMap { case (m, prefix) => s3Client.getPrefixData(prefix).map(o => m -> o.toRef) }
}

object InferenceSubscriptionService {
  type S3Data = (Model, S3Ref)

  def make(
      s3Client: S3Client,
      objIndex: S3ObjectIndex,
      modelRepository: ModelRepository,
      log: Logger[String]
  ): ZIO[Any, Nothing, InferenceSubscriptionService] = for {
    state <- ZRef.make(Map.empty[PluginId, zio.Hub[S3Data]])
    cap = 20 //TODO(bulat): move to config
  } yield InferenceSubscriptionService(log, modelRepository, s3Client, objIndex, state, cap)

  val layer = (for {
    log             <- ZIO.service[Logger[String]]
    s3Client        <- ZIO.service[S3Client]
    objIndex        <- ZIO.service[S3ObjectIndex]
    modelRepository <- ZIO.service[ModelRepository]
    impl            <- make(s3Client, objIndex, modelRepository, log)
  } yield impl).toLayer
}
