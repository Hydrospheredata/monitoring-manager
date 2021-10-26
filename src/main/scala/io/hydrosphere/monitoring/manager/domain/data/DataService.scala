package io.hydrosphere.monitoring.manager.domain.data

import io.github.vigoo.zioaws.s3.model.S3Object
import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}
import io.hydrosphere.monitoring.manager.domain.model.{Model, ModelService}
import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId
import zio.ZIO
import zio.stream.ZStream

object DataService {

  case class NoInferenceData(pluginId: PluginId) extends Error

  def subscibeToInferenceData(
      pluginId: PluginId
  ) =
    for {
      subManager <- ZStream.service[InferenceSubscriptionService]
      hub        <- ZStream.fromEffect(subManager.subscribe(pluginId))
      data       <- ZStream.fromHub(hub)
    } yield data
}
