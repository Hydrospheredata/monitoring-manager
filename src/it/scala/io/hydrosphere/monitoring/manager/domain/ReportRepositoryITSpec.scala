package io.hydrosphere.monitoring.manager.domain

import com.dimafeng.testcontainers.PostgreSQLContainer
import io.hydrosphere.monitoring.manager.db.DatabaseContext
import io.hydrosphere.monitoring.manager.domain.data.S3Ref
import io.hydrosphere.monitoring.manager.domain.report.Report.BatchStats
import io.hydrosphere.monitoring.manager.domain.report.{Report, ReportRepository, ReportRepositoryImpl}
import io.hydrosphere.monitoring.manager.util.URI.Context
import io.hydrosphere.monitoring.manager.{GenericIntegrationTest, MigrationAspects}
import zio.test._
import zio.{Chunk, Has, ZIO, ZLayer}

import java.time.Instant

object ReportRepositoryITSpec extends GenericIntegrationTest {

  val repoLayer: ZLayer[Any, Nothing, Has[ReportRepository]] =
    (MigrationAspects.pgLayer >>> MigrationAspects.dsLayer) ++ DatabaseContext.layer >>> ReportRepositoryImpl.layer

  val testLayer: ZLayer[Any, Nothing, Has[PostgreSQLContainer] with Has[ReportRepository]] =
    MigrationAspects.pgLayer ++
      repoLayer

  val i     = Instant.now()
  val stats = BatchStats(1, "ok", 1)
  val spec = (suite("ReportRepository")(
    testM("should create a report") {
      val path =
        uri"s3://test/file.csv"
      val report = Report(
        pluginId = "test-plugin",
        modelName = "model",
        modelVersion = 1,
        file = path,
        fileModifiedAt = i,
        featureReports = Map(
          "a" -> Seq(Report.ByFeature("ok", true), Report.ByFeature("really-good", true)),
          "b" -> Seq(Report.ByFeature("not-ok", false), Report.ByFeature("really-not-good", false))
        ),
        batchStats = Some(stats)
      )
      val res = ReportRepository.create(report)
      assertM(res)(Assertion.equalTo(Chunk(report)))
    },
    testM("should return specific report") {
      val report = Report(
        pluginId = "test-plugin",
        modelName = "model",
        modelVersion = 2,
        file = uri"s3://test/specific.csv",
        fileModifiedAt = i,
        featureReports = Map(
          "a" -> Seq(Report.ByFeature("ok", true), Report.ByFeature("really-good", true)),
          "b" -> Seq(Report.ByFeature("not-ok", false), Report.ByFeature("really-not-good", false))
        ),
        batchStats = Some(stats)
      )
      val res =
        ReportRepository.create(report) *> ReportRepository.get("model", 2, uri"s3://test/specific.csv").runCollect
      assertM(res)(Assertion.equalTo(Chunk(report)))
    },
    testM("should return all inference files for a model") {
      val reports = Chunk(
        Report(
          pluginId = "test-plugin-1",
          modelName = "model",
          modelVersion = 3,
          file = uri"s3://test/specific1.csv",
          fileModifiedAt = i,
          featureReports = Map(
            "a" -> Seq(Report.ByFeature("ok", true), Report.ByFeature("really-good", true)),
            "b" -> Seq(Report.ByFeature("not-ok", false), Report.ByFeature("really-not-good", false))
          ),
          batchStats = Some(stats)
        ),
        Report(
          pluginId = "test-plugin-2",
          modelName = "model",
          modelVersion = 3,
          file = uri"s3://test/specific1.csv",
          fileModifiedAt = i,
          featureReports = Map(
            "a" -> Seq(Report.ByFeature("ok", true), Report.ByFeature("really-good", true)),
            "b" -> Seq(Report.ByFeature("not-ok", false), Report.ByFeature("really-not-good", false))
          ),
          batchStats = Some(stats)
        ),
        Report(
          pluginId = "test-plugin-1",
          modelName = "model",
          modelVersion = 3,
          file = uri"s3://test/specific2.csv",
          fileModifiedAt = i,
          featureReports = Map(
            "a" -> Seq(Report.ByFeature("ok", true), Report.ByFeature("really-good", true)),
            "b" -> Seq(Report.ByFeature("not-ok", false), Report.ByFeature("really-not-good", false))
          ),
          batchStats = Some(stats)
        )
      )
      val expected = Set(uri"s3://test/specific1.csv", uri"s3://test/specific2.csv")
      val prog =
        ZIO.foreach_(reports)(ReportRepository.create) *> ReportRepository.peekForModelVersion("model", 3).runCollect
      assertM(prog.map(_.toSet))(Assertion.equalTo(expected))
    },
    testM("should check if plugin submited a report") {
      val path = uri"s3://test/file.csv"
      val report = Report(
        pluginId = "test-plugin",
        modelName = "model",
        modelVersion = 1,
        file = path,
        fileModifiedAt = i,
        featureReports = Map(
          "a" -> Seq(Report.ByFeature("ok", true), Report.ByFeature("really-good", true)),
          "b" -> Seq(Report.ByFeature("not-ok", false), Report.ByFeature("really-not-good", false))
        ),
        batchStats = Some(stats)
      )
      val res = ReportRepository.create(report) *> ReportRepository
        .exists("test-plugin", S3Ref(path, i))
        .zip(ReportRepository.exists("test-plugin-1", S3Ref(path, i)))
      assertM(res)(Assertion.equalTo(true -> false))
    }
  ) @@ MigrationAspects.migrate()).provideCustomLayerShared(testLayer)
}
