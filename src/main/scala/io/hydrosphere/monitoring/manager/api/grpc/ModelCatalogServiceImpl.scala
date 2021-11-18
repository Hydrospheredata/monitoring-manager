package io.hydrosphere.monitoring.manager.api.grpc

import io.grpc.Status
import io.hydrosphere.monitoring.manager.domain.model.ModelSubscriptionManager
import monitoring_manager.monitoring_manager._
import monitoring_manager.monitoring_manager.ZioMonitoringManager.ModelCatalogService
import zio._
import zio.stream.ZStream

final case class ModelCatalogServiceImpl(modelSubscriptionManager: ModelSubscriptionManager)
    extends ModelCatalogService {
  override def getModelUpdates(request: GetModelUpdatesRequest): ZStream[Any, Status, GetModelUpdatesResponse] =
    modelSubscriptionManager
      .subscribe(request.pluginId)
      .mapBoth(
        x => Status.INTERNAL.withCause(x),
        model =>
          GetModelUpdatesResponse(
            model = Some(
              ModelId(
                modelName = model.name,
                modelVersion = model.version
              )
            ),
            signature = Some(model.signature.toProto),
            trainingDataObjs = model.trainingDataPrefix.map(x => DataObject(key = x.toString())).toSeq
          )
      )
}

object ModelCatalogServiceImpl {
  val layer: URLayer[Has[ModelSubscriptionManager], Has[ModelCatalogService]] =
    (ModelCatalogServiceImpl.apply _).toLayer
}
