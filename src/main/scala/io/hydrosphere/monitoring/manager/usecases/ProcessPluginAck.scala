package io.hydrosphere.monitoring.manager.usecases

import io.hydrosphere.monitoring.manager.domain.metrics.{Measurable, MetricsService, PushGateway}
import io.hydrosphere.monitoring.manager.domain.metrics.MeasurableInstances._
import io.hydrosphere.monitoring.manager.domain.report.ReportErrors.InvalidAckReport
import io.hydrosphere.monitoring.manager.domain.report.{Report, ReportRepository}
import monitoring_manager.monitoring_manager.GetInferenceDataUpdatesRequest
import zio.{Has, ZIO}
import zio.logging.{log, Logging}

object ProcessPluginAck {
  def apply(
      req: GetInferenceDataUpdatesRequest
  ): ZIO[Has[PushGateway] with Has[ReportRepository] with Logging, Throwable, Report] =
    (for {
      _      <- log.debug(s"Got request: ${req.pluginId} ack=${req.ack.isDefined}")
      report <- parseReport(req)
      _      <- ReportRepository.create(report)
      _ <- MetricsService
        .sendMeasurable(s"${report.pluginId}: ${report.file} ${report.fileModifiedAt}", report)
        .tapError(x => log.throwable("Error while sending metrics", x))
        .forkDaemon
    } yield report)
      .tapError(err => log.throwable("Error while handling plugin request", err))

  def parseReport(req: GetInferenceDataUpdatesRequest) =
    for {
      rawReport <- ZIO.fromOption(req.ack).orElseFail(InvalidAckReport(req.pluginId, s"${req.pluginId} sent empty ack"))
      report <- ZIO
        .fromEither(Report.fromPluginAck(req.pluginId, rawReport))
        .mapError(err => InvalidAckReport(req.pluginId, s"Can't extract inference file information: $err"))
    } yield report
}
