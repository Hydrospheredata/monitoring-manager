package io.hydrosphere.monitoring.manager.domain.metrics

import io.hydrosphere.monitoring.manager.domain.report.Report
import io.hydrosphere.monitoring.manager.domain.report.Report.{BatchStats, FeatureReports}
import zio.ZIO
import zio.metrics.prometheus.{Counter, Registry, Summary}

object MeasurableInstances {
  implicit val batchStats: Measurable[BatchStats] = new Measurable[BatchStats] {
    def measure(obj: BatchStats, parentLabels: MetricLabels): ZIO[Registry, Throwable, Unit] =
      for {
        susGauge  <- Summary("suspicious-items-ratio", parentLabels.toArray, List.empty)
        _         <- susGauge.observe(obj.susRatio)
        failGauge <- Summary("failed-items-ratio", parentLabels.toArray, List.empty)
        _         <- failGauge.observe(obj.failRatio)
      } yield ()
  }

  implicit val feature: Measurable[FeatureReports] = new Measurable[FeatureReports] {
    override def measure(obj: FeatureReports, parentLabels: MetricLabels): ZIO[Registry, Throwable, Unit] =
      for {
        gauge <- Counter("drift-check-fails", parentLabels.toArray)
        _ <- ZIO.foreach_(obj) { case (feature, reports) =>
          val num = reports.filterNot(_.isGood).size
          gauge.inc(num, Array(s"feature=$feature"))
        }
      } yield ()
  }

  implicit val reportMeasurable: Measurable[Report] = new Measurable[Report] {
    override def measure(obj: Report, parentLabels: MetricLabels): ZIO[Registry, Throwable, Unit] = {
      val labels = parentLabels ++ Map(
        "pluginId"     -> obj.pluginId,
        "modelName"    -> obj.modelName,
        "modelVersion" -> obj.modelVersion.toString,
        "file"         -> obj.file.toString,
        "timestamp"    -> obj.fileModifiedAt.toString
      )
      for {
        counter <- Counter("reports", labels.toArray)
        _       <- counter.inc()
        _ <- obj.batchStats match {
          case Some(value) => batchStats.measure(value, labels)
          case None        => ZIO.unit
        }
        _ <- obj.featureReports match {
          case Some(value) if value.nonEmpty =>
            feature.measure(value, labels)
          case _ => ZIO.unit
        }
      } yield ???
    }
  }

}
