package io.hydrosphere.monitoring.manager.api.grpc

import io.grpc.Status
import io.hydrosphere.monitoring.manager.domain.data.{DataService, InferenceSubscriptionService}
import io.hydrosphere.monitoring.manager.domain.model.ModelRepository
import monitoring_manager.monitoring_manager.ZioMonitoringManager.DataStorageService
import monitoring_manager.monitoring_manager._
import monitoring_manager.monitoring_manager.GetInferenceDataUpdatesRequest.Data
import zio.{stream, ZIO}
import zio.stream.{ZSink, ZStream}
import zio.logging.{log, Logger}
import zio._

//NB(bulat): Extend DataStorageService because we don't need extra GRPC context. Yet.
final case class DataStorageServiceImpl(
    log: Logger[String],
    env: ZEnv,
    subscriptionManager: InferenceSubscriptionService,
    modelRepository: ModelRepository
) extends DataStorageService {
  override def getInferenceDataUpdates(
      request: stream.Stream[Status, GetInferenceDataUpdatesRequest]
  ) =
    request
      .map(_.data)
      .flatMap {
        case Data.Empty =>
          val err = log.warn("Empty message") *>
            ZIO.fail(Status.INVALID_ARGUMENT.withDescription("Request cannot be empty"))
          ZStream.fromEffect(err)
        case Data.Init(value) =>
          DataService
            .subscibeToInferenceData(
              value.pluginId,
              value.model.get.modelName,   //TODO(bulat) unsafe
              value.model.get.modelVersion //TODO(bulat) unsafe
            )
            .mapBoth(
              x => Status.INTERNAL.withCause(x),
              el =>
                GetInferenceDataUpdatesResponse(
                  model = Some(
                    ModelId(
                      modelName = el.model.name,
                      modelVersion = el.model.version
                    )
                  ),
                  signature = Some(el.model.signature.toProto),
                  inferenceDataObjs = el.data.keyValue.toSeq
                )
            )
            .provide(Has(log) ++ env ++ Has(subscriptionManager) ++ Has(modelRepository))

        case Data.Ack(value) => ZStream.empty
      }
}

object DataStorageServiceImpl {
  val layer = (for {
    env                 <- ZIO.environment[ZEnv]
    log                 <- ZIO.service[Logger[String]]
    subscriptionManager <- ZIO.service[InferenceSubscriptionService]
    modelRepository     <- ZIO.service[ModelRepository]
  } yield DataStorageServiceImpl.apply(log, env, subscriptionManager, modelRepository)).toLayer
}
