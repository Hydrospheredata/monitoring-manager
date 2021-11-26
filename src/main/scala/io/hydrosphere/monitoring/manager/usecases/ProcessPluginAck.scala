package io.hydrosphere.monitoring.manager.usecases

import io.hydrosphere.monitoring.manager.domain.metrics.{Measurable, MetricsService}
import io.hydrosphere.monitoring.manager.domain.metrics.MeasurableInstances._
import io.hydrosphere.monitoring.manager.domain.report.ReportErrors.InvalidAckReport
import io.hydrosphere.monitoring.manager.domain.report.{Report, ReportRepository}
import monitoring_manager.monitoring_manager.GetInferenceDataUpdatesRequest
import zio.ZIO
import zio.logging.log

object ProcessPluginAck {
  def apply(req: GetInferenceDataUpdatesRequest) =
    (for {
      _      <- log.debug(s"Got request: ${req.pluginId} ack=${req.ack.isDefined}")
      report <- parseReport(req)
      _      <- ReportRepository.create(report)
      _      <- MetricsService.sendMeasurable(s"${report.pluginId}: ${report.file} ${report.fileModifiedAt}", report)
    } yield report)
      .tapError(err => log.throwable("Error while handling plugin request", err))

  def parseReport(req: GetInferenceDataUpdatesRequest) =
    for {
      _         <- log.info(s"${req.pluginId} sent data update. isAck=${req.ack.isDefined}")
      rawReport <- ZIO.fromOption(req.ack).orElseFail(InvalidAckReport(req.pluginId, s"${req.pluginId} sent empty ack"))
      report <- ZIO
        .fromEither(Report.fromPluginAck(req.pluginId, rawReport))
        .mapError(err => InvalidAckReport(req.pluginId, s"Can't extract inference file information: $err"))
    } yield report
}
