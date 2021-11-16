package io.hydrosphere.monitoring.manager.api.grpc

import io.grpc.Status
import io.hydrosphere.monitoring.manager.domain.data.InferenceSubscriptionService.S3Data
import io.hydrosphere.monitoring.manager.domain.data.{DataService, InferenceSubscriptionService, S3Obj}
import io.hydrosphere.monitoring.manager.domain.model.{Model, ModelRepository}
import io.hydrosphere.monitoring.manager.domain.report.ReportRepository
import monitoring_manager.monitoring_manager._
import monitoring_manager.monitoring_manager.GetInferenceDataUpdatesRequest.Data
import monitoring_manager.monitoring_manager.ZioMonitoringManager.DataStorageService
import zio._
import zio.logging.Logger
import zio.stream.ZStream

final case class DataStorageServiceImpl(
    log: Logger[String],
    subscriptionManager: InferenceSubscriptionService,
    reportRepository: ReportRepository
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
            .tap(data => log.info(s"${value.pluginId} data: ${data.toProtoString}"))
            .ensuring(log.info(s"Stream for ${value.pluginId} plugin finished"))
            .provide(Has(log) ++ Has(subscriptionManager))

        case Data.Ack(value) =>
          DataService.subscibeToInferenceData(value.pluginId).provide(Has(subscriptionManager))
      }
}

object DataStorageServiceImpl {
  val layer =
    (DataStorageServiceImpl.apply _).toLayer
}
