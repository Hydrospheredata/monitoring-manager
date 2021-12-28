package io.hydrosphere.monitoring.manager.domain.data

import io.hydrosphere.monitoring.manager.domain.data.InferenceSubscriptionService.S3Data
import io.hydrosphere.monitoring.manager.domain.model.{Model, ModelRepository}
import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId
import io.hydrosphere.monitoring.manager.domain.report.ReportRepository
import zio.clock.Clock
import zio.logging.{LogAnnotation, Logger}
import zio.random._
import zio.stream.ZStream
import zio.{Has, Ref, Schedule, ZHub, ZIO, ZRef}

import scala.concurrent.duration._
import scala.jdk.DurationConverters._

case class InferenceSubscriptionService(
    logger: Logger[String],
    rng: Random.Service,
    modelRepository: ModelRepository,
    s3Client: S3Client,
    objIndex: S3ObjectIndex,
    hubsState: Ref[Map[PluginId, zio.Hub[S3Data]]],
    cap: Int
) {

  def hubGetOrSet(pluginId: PluginId) = {
    val getHub = hubsState.get
      .flatMap(m => ZIO.fromOption(m.get(pluginId))) <* logger.debug(s"Got $pluginId hub from cache")
    val createHub = for {
      hub <- ZHub.bounded[S3Data](cap)
      _   <- hubsState.update(m => m + (pluginId -> hub))
      _   <- logger.debug(s"Created $pluginId hub")
    } yield hub
    getHub.orElse(createHub)
  }

  def subscribe(pluginId: PluginId) =
    ZStream.fromEffect(hubGetOrSet(pluginId)).flatMap(x => ZStream.fromHub(x))

  def startMonitoring(
      interval: FiniteDuration = 10.seconds
  ) = {
    val iteration =
      monitoringStep.tap { case (model, obj) =>
        for {
          _        <- logger.debug("Got the subscription state")
          stateMap <- hubsState.get
          _ <- ZIO.foreachPar_(stateMap.toSeq) { case (pluginId, hub) =>
            ((hub.publish(model -> obj) *> logger.info(s"Discovered ${obj.fullPath} for $pluginId"))
              .whenM(shouldSend(pluginId, obj)))
          }
        } yield ()
      }.runDrain
    rng.nextUUID
      .flatMap(id => logger.locally(LogAnnotation.CorrelationId(Some(id)))(iteration))
      .schedule(Schedule.spaced(interval.toJava))
  }

  def shouldSend(
      pluginId: PluginId,
      obj: S3Ref
  ): ZIO[Clock with Has[ReportRepository], Throwable, Boolean] =
    ReportRepository.exists(pluginId, obj).flatMap {
      case true =>
        logger.debug(s"$pluginId has a report for $obj. Won't send.") *>
          ZIO(false)
      case false =>
        objIndex.isNew(pluginId, obj).tap(isNew => logger.debug(s"Index: $obj -> $pluginId? $isNew"))
    }

  def monitoringStep: ZStream[Has[ModelRepository], Throwable, S3Data] =
    ModelRepository
      .all()
      .tap(m => logger.debug(s"Discovering ${m.name}:${m.version} model"))
      .map(x => x -> x.inferenceDataPrefix)
      .collect { case (m, Some(prefix)) => m -> prefix }
      .flatMap { case (m, prefix) => s3Client.getPrefixData(prefix).map(o => m -> o.toRef) }
      .tapError(err => logger.throwable("S3 Monitoring iteration failed", err))
      .ensuring(logger.debug("S3 Monitoring iteration finished"))
}

object InferenceSubscriptionService {
  type S3Data = (Model, S3Ref)

  def make(
      s3Client: S3Client,
      objIndex: S3ObjectIndex,
      modelRepository: ModelRepository,
      log: Logger[String],
      rng: Random.Service
  ): ZIO[Any, Nothing, InferenceSubscriptionService] = for {
    state <- ZRef.make(Map.empty[PluginId, zio.Hub[S3Data]])
    cap = 20 //TODO(bulat): move to config
  } yield InferenceSubscriptionService(log, rng, modelRepository, s3Client, objIndex, state, cap)

  val layer = (for {
    log             <- ZIO.service[Logger[String]]
    rng             <- ZIO.service[Random.Service]
    s3Client        <- ZIO.service[S3Client]
    objIndex        <- ZIO.service[S3ObjectIndex]
    modelRepository <- ZIO.service[ModelRepository]
    impl            <- make(s3Client, objIndex, modelRepository, log, rng)
  } yield impl).toLayer
}
