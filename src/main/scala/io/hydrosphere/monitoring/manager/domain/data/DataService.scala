package io.hydrosphere.monitoring.manager.domain.data

import io.hydrosphere.monitoring.manager.domain.model.Model
import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId
import monitoring_manager.monitoring_manager.{GetInferenceDataUpdatesResponse, ModelId}
import zio.stream.ZStream
import zio.logging.log

object DataService {

  case class NoInferenceData(pluginId: PluginId) extends Error

  def subscibeToInferenceData(
      pluginId: PluginId
  ) =
    (for {
      subManager   <- ZStream.service[InferenceSubscriptionService]
      (model, obj) <- subManager.subscribe(pluginId)
    } yield mapToGrpc(model, obj))
      .ensuring(log.info(s"Stream for $pluginId plugin finished"))

  def mapToGrpc(model: Model, obj: S3Obj) =
    GetInferenceDataUpdatesResponse(
      model = Some(
        ModelId(
          modelName = model.name,
          modelVersion = model.version
        )
      ),
      signature = Some(model.signature.toProto),
      inferenceDataObjs = Seq(obj.fullPath.toString())
    )
}
