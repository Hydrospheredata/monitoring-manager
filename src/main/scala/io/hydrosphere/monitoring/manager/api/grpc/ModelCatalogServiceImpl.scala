package io.hydrosphere.monitoring.manager.api.grpc

import io.grpc.Status
import io.hydrosphere.monitoring.manager.domain.model.ModelSubscriptionManager
import monitoring_manager.monitoring_manager._
import monitoring_manager.monitoring_manager.ZioMonitoringManager.ModelCatalogService
import zio._

final case class ModelCatalogServiceImpl(modelSubscriptionManager: ModelSubscriptionManager)
    extends ModelCatalogService {
  override def getModelUpdates(request: GetModelUpdatesRequest) =
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
            trainingDataObjs = model.trainingDataPrefix.map(_.toString()).toSeq
          )
      )
}

object ModelCatalogServiceImpl {
  val layer: URLayer[Has[ModelSubscriptionManager], Has[ModelCatalogService]] =
    (ModelCatalogServiceImpl.apply _).toLayer
}
