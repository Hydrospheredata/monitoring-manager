package io.hydrosphere.monitoring.manager.domain.report

import io.hydrosphere.monitoring.manager.domain.report.ReportErrors.InvalidAckReport
import monitoring_manager.monitoring_manager.GetInferenceDataUpdatesRequest
import zio._
import zio.logging.{log, Logging}

object ReportService {
  def parseReport(req: GetInferenceDataUpdatesRequest): ZIO[Logging, InvalidAckReport, Report] =
    for {
      _         <- log.info(s"${req.pluginId} sent data update. isAck=${req.ack.isDefined}")
      rawReport <- ZIO.fromOption(req.ack).orElseFail(InvalidAckReport(req.pluginId, s"${req.pluginId} sent empty ack"))
      report <- ZIO
        .fromEither(Report.fromPluginAck(req.pluginId, rawReport))
        .mapError(err => InvalidAckReport(req.pluginId, s"Can't extract inference file information: $err"))
    } yield report

  def addReport(
      req: GetInferenceDataUpdatesRequest
  ): ZIO[Has[ReportRepository] with Logging, Throwable, Report] =
    (parseReport(req) >>= ReportRepository.create).tapError(err => log.throwable("Error while submitting report", err))
}
