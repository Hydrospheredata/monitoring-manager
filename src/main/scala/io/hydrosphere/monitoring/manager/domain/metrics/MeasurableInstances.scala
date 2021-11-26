package io.hydrosphere.monitoring.manager.domain.metrics

import io.hydrosphere.monitoring.manager.domain.report.Report
import io.hydrosphere.monitoring.manager.domain.report.Report.{BatchStats, FeatureReports}
import zio.ZIO
import zio.metrics.prometheus.{Counter, Registry, Summary}

object MeasurableInstances {
  implicit val batchStats: Measurable[BatchStats] = new Measurable[BatchStats] {
    def measure(obj: BatchStats, parentLabels: MetricLabels): ZIO[Registry, Throwable, Unit] =
      for {
        susGauge  <- Summary("SuspiciousItemsRatio", parentLabels.keys, List.empty)
        _         <- susGauge.observe(obj.susRatio, parentLabels.values)
        failGauge <- Summary("failedItemsRatio", parentLabels.keys, List.empty)
        _         <- failGauge.observe(obj.failRatio, parentLabels.values)
      } yield ()
  }

  implicit val feature: Measurable[FeatureReports] = new Measurable[FeatureReports] {
    override def measure(obj: FeatureReports, parentLabels: MetricLabels): ZIO[Registry, Throwable, Unit] =
      for {
        gauge <- Counter("DriftCheckFails", parentLabels.keys)
        _ <- ZIO.foreach_(obj) { case (feature, reports) =>
          val num = reports.filterNot(_.isGood).size
          gauge.inc(num, parentLabels.values)
        }
      } yield ()
  }

  implicit val reportMeasurable: Measurable[Report] = new Measurable[Report] {
    override def measure(obj: Report, parentLabels: MetricLabels): ZIO[Registry, Throwable, Unit] = {
      val labels = parentLabels ++ Map(
        "pluginId"     -> obj.pluginId,
        "modelName"    -> obj.modelName,
        "modelVersion" -> obj.modelVersion.toString,
        "file"         -> obj.file.toString
      )
      for {
        counter <- Counter("reports", labels.keys)
        _       <- counter.inc(labels.values)
        _ <- obj.batchStats match {
          case Some(value) => batchStats.measure(value, labels)
          case None        => ZIO.unit
        }
        _ <- obj.featureReports match {
          case Some(value) if value.nonEmpty =>
            feature.measure(value, labels)
          case _ => ZIO.unit
        }
      } yield ()
    }
  }

}
