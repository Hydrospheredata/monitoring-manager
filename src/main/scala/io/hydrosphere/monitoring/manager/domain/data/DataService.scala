package io.hydrosphere.monitoring.manager.domain.data

import com.google.protobuf.timestamp.Timestamp
import io.hydrosphere.monitoring.manager.domain.model.Model
import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId
import monitoring_manager.monitoring_manager.{DataObject, GetInferenceDataUpdatesResponse, ModelId}
import zio.logging.log
import zio.stream.ZStream

object DataService {

  case class NoInferenceData(pluginId: PluginId) extends Error

  def subscibeToInferenceData(
      pluginId: PluginId
  ) =
    (for {
      subManager   <- ZStream.service[InferenceSubscriptionService]
      (model, obj) <- subManager.subscribe(pluginId)
    } yield mapToGrpc(model, Seq(obj)))
      .ensuring(log.info(s"Stream for $pluginId plugin finished"))

  def mapToGrpc(model: Model, objs: Seq[S3Ref]) =
    GetInferenceDataUpdatesResponse(
      model = Some(
        ModelId(
          modelName = model.name,
          modelVersion = model.version
        )
      ),
      signature = Some(model.signature.toProto),
      inferenceDataObjs =
        objs.map(o => DataObject(key = o.fullPath.toString, lastModifiedAt = Some(Timestamp(o.lastModified))))
    )
}
