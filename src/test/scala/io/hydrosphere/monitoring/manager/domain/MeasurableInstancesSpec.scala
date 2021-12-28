package io.hydrosphere.monitoring.manager.domain

import io.hydrosphere.monitoring.manager.GenericUnitTest
import io.hydrosphere.monitoring.manager.domain.metrics.{MeasurableInstances, MetricLabels}
import io.hydrosphere.monitoring.manager.domain.report.Report
import io.hydrosphere.monitoring.manager.domain.report.Report.BatchStats
import io.hydrosphere.monitoring.manager.util.URI.Context
import zio.test.{assertM, Assertion}

import java.time.Instant

object MeasurableInstancesSpec extends GenericUnitTest {
  val report = Report(
    pluginId = "test-plugin",
    modelName = "model",
    modelVersion = 2,
    file = uri"s3://test/specific.csv",
    fileModifiedAt = Instant.now(),
    featureReports = Some(
      Map(
        "a" -> Seq(Report.ByFeature("ok", true), Report.ByFeature("really-good", true)),
        "b" -> Seq(Report.ByFeature("not-ok", false), Report.ByFeature("really-not-good", false))
      )
    ),
    batchStats = Some(BatchStats(1, "ok", 1))
  )

  val spec = suite("Measurable[Report]") {
    testM("should convert to metrics") {
      val result = MeasurableInstances.reportMeasurable(report)
      assertM(result)(Assertion.anything)
    }
  }
}
