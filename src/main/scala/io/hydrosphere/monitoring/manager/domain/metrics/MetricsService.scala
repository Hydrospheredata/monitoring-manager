package io.hydrosphere.monitoring.manager.domain.metrics

import io.hydrosphere.monitoring.manager.domain.metrics.sender.MetricSender
import io.hydrosphere.monitoring.manager.domain.report.Report
import zio.logging.log

object MetricsService {
  def sendMeasurable[T](report: Report) =
    for {
      _       <- log.debug(s"Exporting report metrics for plugin=${report.pluginId} file=${report.file}")
      metrics <- MeasurableInstances.reportMeasurable(report)
      _       <- log.trace(s"Sending metrics: $metrics")
      _       <- MetricSender.push(metrics)
    } yield ()
}
