package io.hydrosphere.monitoring.manager.api.grpc

import io.grpc.Status
import io.hydrosphere.monitoring.manager.domain.data.{DataService, InferenceSubscriptionService}
import io.hydrosphere.monitoring.manager.domain.metrics.PushGateway
import io.hydrosphere.monitoring.manager.domain.report.ReportRepository
import io.hydrosphere.monitoring.manager.usecases.ProcessPluginAck
import monitoring_manager.monitoring_manager.ZioMonitoringManager.RDataStorageService
import monitoring_manager.monitoring_manager._
import zio._
import zio.logging.{log, Logging}
import zio.stream.ZStream

final case class DataStorageServiceImpl()
    extends RDataStorageService[
      Logging with Has[InferenceSubscriptionService] with Has[PushGateway] with Has[ReportRepository]
    ] {
  override def getInferenceDataUpdates(
      request: stream.Stream[Status, GetInferenceDataUpdatesRequest]
  ): ZStream[Logging with Has[InferenceSubscriptionService] with Has[PushGateway] with Has[
    ReportRepository
  ], Status, GetInferenceDataUpdatesResponse] = {
    //noinspection SimplifyTapInspection (bug in idea-zio)
    val requestHandling = request
      .mapError(_.asRuntimeException())
      .tap(req => log.debug(s"Got request: ${req.pluginId} ack=${req.ack.isDefined}"))
      .tap(ProcessPluginAck(_).either)
      .tapError(err => log.throwable("Error while handling plugin request", err))

    request
      .tap(ProcessPluginAck(_).either)
      .mapError(_.asRuntimeException())
      .flatMap(r => requestHandling.mergeEither(DataService.subscibeToInferenceData(r.pluginId)))
      .collect { case Right(v) => v }
      .mapError(Status.fromThrowable)
  }
}

object DataStorageServiceImpl {
  val layer =
    (DataStorageServiceImpl.apply _).toLayer
}
