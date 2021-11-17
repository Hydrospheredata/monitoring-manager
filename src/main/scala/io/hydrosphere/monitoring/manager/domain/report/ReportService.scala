package io.hydrosphere.monitoring.manager.domain.report

import monitoring_manager.monitoring_manager.GetInferenceDataUpdatesRequest
import zio._
import zio.logging.{log, Logging}

object ReportService {
  def addReport(
      req: GetInferenceDataUpdatesRequest
  ): ZIO[Has[ReportRepository] with Logging, Nothing, Option[Report]] =
    req.ack match {
      case Some(value) =>
        val report = Report.fromPluginAck(req.pluginId, value)
        log.info(
          s"${req.pluginId} sent ack with report for ${value.modelName}:${value.modelVersion} - ${value.inferenceDataObj}"
        ) *>
          ReportRepository
            .create(report)
            .map(Some.apply)
            .orElse(log.error(s"Couldn't safe report $report").as(None))
      case None =>
        log.info(s"${req.pluginId} sent empty ack") *>
          ZIO.none
    }
}
