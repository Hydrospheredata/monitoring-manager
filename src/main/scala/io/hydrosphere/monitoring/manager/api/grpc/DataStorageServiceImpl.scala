package io.hydrosphere.monitoring.manager.api.grpc

import io.grpc.Status
import io.hydrosphere.monitoring.manager.domain.data.{DataService, InferenceSubscriptionService}
import io.hydrosphere.monitoring.manager.domain.report.{ReportRepository, ReportService}
import monitoring_manager.monitoring_manager.ZioMonitoringManager.DataStorageService
import monitoring_manager.monitoring_manager._
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
  ) = {
    //noinspection SimplifyTapInspection (bug in idea-zio)
    val ackFbr = request
      .mapError(_.asRuntimeException())
      .tap(req => log.debug(s"Got request: ${req.pluginId} ack=${req.ack.isDefined}"))
      .tap(ReportService.addReport(_).either)
      .tapError(err => log.throwable("Error while handling plugin request", err))
      .mapError(Status.fromThrowable)
      .runDrain
      .fork

    val a = request
      .mapError(_.asRuntimeException())
      .tap(req => log.debug(s"Got request: ${req.pluginId} ack=${req.ack.isDefined}"))
      .tap(ReportService.addReport(_).either)
      .tapError(err => log.throwable("Error while handling plugin request", err))
      .zipRight(ZStream.empty)

    val b = request
      .mapError(_.asRuntimeException())
      .flatMap(r => DataService.subscibeToInferenceData(r.pluginId))

    a.merge(b)
      .mapError(Status.fromThrowable)
      .provide(Has(log) ++ Has(subscriptionManager) ++ Has(reportRepository))
  }
}

object DataStorageServiceImpl {
  val layer =
    (DataStorageServiceImpl.apply _).toLayer
}
