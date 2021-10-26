package io.hydrosphere.monitoring.manager.domain.data

import io.github.vigoo.zioaws.s3.model.S3Object
import io.hydrosphere.monitoring.manager.domain.data.InferenceSubscriptionService.S3Data
import io.hydrosphere.monitoring.manager.domain.model.{Model, ModelRepository}
import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId
import zio.{Has, Ref, Schedule, ZHub, ZIO, ZRef}
import zio.logging.log
import zio.stream.ZStream

import java.time.Duration

case class InferenceSubscriptionService(
    modelRepository: ModelRepository,
    s3Client: S3Client,
    objIndex: S3ObjectIndex,
    state: Ref[Map[PluginId, zio.Hub[S3Data]]],
    cap: Int
) {
  def subscribe(pluginId: PluginId) = {
    val get = state.get
      .flatMap(m => ZIO.fromOption(m.get(pluginId))) <* log.debug(s"Got $pluginId hub from cache")
    val create = for {
      hub <- ZHub.bounded[S3Data](cap)
      _   <- state.update(m => m + (pluginId -> hub))
      _   <- log.debug(s"Created $pluginId hub")
    } yield hub
    get.orElse(create)
  }

  def unsubscribe(pluginId: PluginId) =
    state.update(stateMap => stateMap - pluginId)

  def startMonitoring =
    monitoringStep
      .tap { case (model, obj) =>
        state.get.flatMap { stateMap =>
          ZIO
            .foreach(stateMap.toSeq) { case (pluginId, hub) =>
              hub.publish(model -> obj).whenM(objIndex.isNew(pluginId, obj))
            }
            .unit
        }
      }
      .schedule(Schedule.spaced(Duration.ofSeconds(20)))

  def monitoringStep: ZStream[Has[ModelRepository], Throwable, S3Data] =
    ModelRepository
      .all()
      .map(x => x -> x.inferenceDataPrefix)
      .collect { case (m, Some(prefix)) => m -> prefix }
      .flatMap { case (m, prefix) => s3Client.getPrefixData(prefix.u).map(o => m -> o) }
}

object InferenceSubscriptionService {
  type S3Data = (Model, S3Object.ReadOnly)

  def make(
      s3Client: S3Client,
      objIndex: S3ObjectIndex,
      modelRepository: ModelRepository
  ): ZIO[Any, Nothing, InferenceSubscriptionService] = for {
    state <- ZRef.make(Map.empty[PluginId, zio.Hub[S3Data]])
    cap = 20 //TODO(bulat): move to config
  } yield InferenceSubscriptionService(modelRepository, s3Client, objIndex, state, cap)

  val layer = (for {
    s3Client        <- ZIO.service[S3Client]
    objIndex        <- ZIO.service[S3ObjectIndex]
    modelRepository <- ZIO.service[ModelRepository]
    impl            <- make(s3Client, objIndex, modelRepository)
  } yield impl).toLayer
}
