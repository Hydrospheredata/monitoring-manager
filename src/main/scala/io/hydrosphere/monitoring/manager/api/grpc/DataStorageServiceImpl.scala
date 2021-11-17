package io.hydrosphere.monitoring.manager.api.grpc

import io.grpc.Status
import io.hydrosphere.monitoring.manager.domain.data.{DataService, InferenceSubscriptionService}
import io.hydrosphere.monitoring.manager.domain.report.{ReportRepository, ReportService}
import monitoring_manager.monitoring_manager.ZioMonitoringManager.DataStorageService
import monitoring_manager.monitoring_manager._
import zio._
import zio.logging.Logger

final case class DataStorageServiceImpl(
    log: Logger[String],
    subscriptionManager: InferenceSubscriptionService,
    reportRepository: ReportRepository
) extends DataStorageService {
  override def getInferenceDataUpdates(
      request: stream.Stream[Status, GetInferenceDataUpdatesRequest]
  ) =
    request
      .tap(req => ReportService.addReport(req))
      .flatMap(req => DataService.subscibeToInferenceData(req.pluginId))
      .provide(Has(log) ++ Has(subscriptionManager) ++ Has(reportRepository))
}

object DataStorageServiceImpl {
  val layer =
    (DataStorageServiceImpl.apply _).toLayer
}
