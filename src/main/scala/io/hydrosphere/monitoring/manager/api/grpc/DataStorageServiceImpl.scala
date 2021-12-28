package io.hydrosphere.monitoring.manager.api.grpc

import io.grpc.Status
import io.hydrosphere.monitoring.manager.domain.data.{DataService, InferenceSubscriptionService}
import io.hydrosphere.monitoring.manager.domain.metrics.sender.MetricSender
import io.hydrosphere.monitoring.manager.domain.report.ReportRepository
import io.hydrosphere.monitoring.manager.usecases.ProcessPluginAck
import monitoring_manager.monitoring_manager.ZioMonitoringManager.RDataStorageService
import monitoring_manager.monitoring_manager._
import zio._
import zio.logging.{log, Logging}
import zio.stream.{ZSink, ZStream}

final case class DataStorageServiceImpl()
    extends RDataStorageService[
      Logging with Has[InferenceSubscriptionService] with Has[MetricSender] with Has[ReportRepository]
    ] {
  override def getInferenceDataUpdates(
      request: stream.Stream[Status, GetInferenceDataUpdatesRequest]
  ): ZStream[Logging with Has[InferenceSubscriptionService] with Has[MetricSender] with Has[
    ReportRepository
  ], Status, GetInferenceDataUpdatesResponse] =
    for {
      q    <- ZStream.fromEffect(Queue.bounded[GetInferenceDataUpdatesRequest](32))
      _    <- ZStream.fromEffect(request.run(ZSink.fromQueue(q)).fork)
      req  <- ZStream.fromEffect(q.take).tap(r => log.debug(s"${r.pluginId} took 1 element for discovery"))
      resp <- DataService.subscibeToInferenceData(req.pluginId)
      _    <- ZStream.fromEffect(q.take.flatMap(ProcessPluginAck.apply).fork)
    } yield resp
}
object DataStorageServiceImpl {
  val layer =
    (DataStorageServiceImpl.apply _).toLayer
}
