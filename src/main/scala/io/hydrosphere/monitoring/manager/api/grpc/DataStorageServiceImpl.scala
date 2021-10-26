package io.hydrosphere.monitoring.manager.api.grpc

import io.grpc.Status
import io.hydrosphere.monitoring.manager.domain.data.{DataService, InferenceSubscriptionService}
import io.hydrosphere.monitoring.manager.domain.model.ModelRepository
import monitoring_manager.monitoring_manager._
import monitoring_manager.monitoring_manager.GetInferenceDataUpdatesRequest.Data
import monitoring_manager.monitoring_manager.ZioMonitoringManager.DataStorageService
import zio._
import zio.logging.Logger
import zio.stream.ZStream

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
              value.pluginId
            )
            .map { case (model, obj) =>
              GetInferenceDataUpdatesResponse(
                model = Some(
                  ModelId(
                    modelName = model.name,
                    modelVersion = model.version
                  )
                ),
                signature = Some(model.signature.toProto),
                inferenceDataObjs = obj.keyValue.toSeq
              )
            }
            .provide(Has(log) ++ env ++ Has(subscriptionManager) ++ Has(modelRepository))

        case Data.Ack(value) => ZStream.empty
      }
}

object DataStorageServiceImpl {
  val layer: ZLayer[Has[ModelRepository] with Has[InferenceSubscriptionService] with Has[
    Logger[String]
  ] with zio.ZEnv, Nothing, Has[DataStorageService]] = (for {
    env                 <- ZIO.environment[ZEnv]
    log                 <- ZIO.service[Logger[String]]
    subscriptionManager <- ZIO.service[InferenceSubscriptionService]
    modelRepository     <- ZIO.service[ModelRepository]
  } yield DataStorageServiceImpl.apply(log, env, subscriptionManager, modelRepository)).toLayer
}
