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
    val discStream = ZStream.fromEffect(request.runHead).flatMap {
      case Some(value) => DataService.subscibeToInferenceData(value.pluginId)
      case None        => ZStream.fromEffect(log.warn("Discovery with empty stream")) *> ZStream.empty
    }

    //noinspection SimplifyTapInspection (bug in idea-zio)
    val ackFbr = request
      .mapError(_.asRuntimeException())
      .tap(req => log.debug(s"Got request: ${req.pluginId} ack=${req.ack.isDefined}"))
      .tap(DataService.markObjSeen)
      .tap(ReportService.addReport(_).either)
      .tapError(err => log.throwable("Error while handling plugin request", err))
      .mapError(Status.fromThrowable)
      .runDrain
      .fork

    (ZStream.fromEffect(ackFbr) >>= (f => discStream.interruptWhen(f.join)))
      .provide(Has(log) ++ Has(subscriptionManager) ++ Has(reportRepository))
  }
}

object DataStorageServiceImpl {
  val layer =
    (DataStorageServiceImpl.apply _).toLayer
}
