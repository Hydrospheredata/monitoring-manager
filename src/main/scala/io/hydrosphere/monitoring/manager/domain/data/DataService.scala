package io.hydrosphere.monitoring.manager.domain.data

import io.github.vigoo.zioaws.s3.model.S3Object
import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}
import io.hydrosphere.monitoring.manager.domain.model.{Model, ModelService}
import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId
import zio.ZIO
import zio.stream.ZStream

object DataService {
  case class DiscoveryEvent(model: Model, data: S3Object.ReadOnly)

  case class NoInferenceData(pluginId: PluginId, modelName: ModelName, modelVersion: ModelVersion)
      extends Error

  def subscibeToInferenceData(
      pluginId: PluginId,
      modelName: ModelName,
      modelVersion: ModelVersion
  ) =
    for {
      mv         <- ZStream.fromEffect(ModelService.findModel(modelName, modelVersion))
      subManager <- ZStream.service[InferenceSubscriptionService]
      inferenceData <- mv.inferenceDataPrefix match {
        case Some(value) => ZStream(value)
        case None        => ZStream.fail(NoInferenceData(pluginId, modelName, modelVersion))
      }
      data <- subManager.subscribe(pluginId, inferenceData.u)
    } yield DiscoveryEvent(mv, data)
}
