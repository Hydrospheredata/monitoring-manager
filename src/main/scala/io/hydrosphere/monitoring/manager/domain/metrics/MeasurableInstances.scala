package io.hydrosphere.monitoring.manager.domain.metrics

import io.hydrosphere.monitoring.manager.domain.report.Report
import io.hydrosphere.monitoring.manager.domain.report.Report.{BatchStats, FeatureReports}
import zio.ZIO

import java.time.Instant

object MeasurableInstances {
  def batchStats(obj: BatchStats, parentLabels: Map[String, String], timestamp: Instant) = ZIO.succeed {
    Seq(
      Metric(
        name = "suspicious_records_ratio",
        value = obj.susRatio,
        description = "Ratio of suspicious records in batch",
        unit = "",
        labels = parentLabels,
        timestamp = timestamp
      ),
      Metric(
        name = "failed_records_ratio",
        value = obj.failRatio,
        description = "Ratio of failed records in batch",
        unit = "",
        labels = parentLabels,
        timestamp = timestamp
      )
    )
  }

  def feature(obj: FeatureReports, parentLabels: Map[String, String], timestamp: Instant) = ZIO.succeed {
    val driftedNum = obj.toSeq.map { case (_, reports) =>
      if (reports.exists(!_.isGood)) 1 else 0
    }.sum
    Seq(
      Metric(
        name = "drifted_features",
        value = driftedNum,
        description = "Number of drifted features in a batch",
        unit = "",
        labels = parentLabels,
        timestamp = timestamp
      )
    )

  }

  def reportMeasurable(obj: Report) = {
    val labels = Map(
      "pluginId"     -> obj.pluginId,
      "modelName"    -> obj.modelName,
      "modelVersion" -> obj.modelVersion.toString,
      "file"         -> obj.file.toString
    )
    val reports_counter = Metric("reports", 1, "number of batch reports", "", labels, obj.fileModifiedAt)

    for {
      batchMetrics <- obj.batchStats match {
        case Some(value) => batchStats(value, labels, obj.fileModifiedAt)
        case None        => ZIO.succeed(Seq.empty)
      }
      featureMetrics <- obj.featureReports match {
        case Some(value) if value.nonEmpty =>
          feature(value, labels, obj.fileModifiedAt)
        case _ => ZIO.succeed(Seq.empty)
      }
    } yield Seq(reports_counter) ++ batchMetrics ++ featureMetrics
  }

}
