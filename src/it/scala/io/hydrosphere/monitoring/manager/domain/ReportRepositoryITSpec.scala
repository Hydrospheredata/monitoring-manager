package io.hydrosphere.monitoring.manager.domain

import com.dimafeng.testcontainers.PostgreSQLContainer
import io.circe.Json
import io.hydrosphere.monitoring.manager.db.DatabaseContext
import io.hydrosphere.monitoring.manager.domain.report.{Report, ReportRepository, ReportRepositoryImpl}
import io.hydrosphere.monitoring.manager.{GenericIntegrationTest, MigrationAspects, TestContainer}
import zio.blocking.Blocking
import zio.test._
import zio.{Chunk, Has, ZIO, ZLayer}

object Deps {
  val pgLayer: ZLayer[Any, Nothing, Has[PostgreSQLContainer]] = Blocking.live >>> TestContainer.postgres
  val repoLayer: ZLayer[Any, Nothing, Has[ReportRepository]] =
    (pgLayer >>> MigrationAspects.dsLayer) ++ DatabaseContext.layer >>> ReportRepositoryImpl.layer

  val testLayer: ZLayer[Any, Nothing, Has[PostgreSQLContainer] with Has[ReportRepository]] =
    pgLayer ++
      repoLayer
}

object ReportRepositoryITSpec extends GenericIntegrationTest {
  val spec = (suite("ReportRepository")(
    testM("should create a report") {
      val report = Report(
        pluginId = "test-plugin",
        modelName = "model",
        modelVersion = 1,
        file = "s3://test/file.csv",
        rowReports = Seq(
          Report.ByRow(
            rowId = 1,
            col = "a",
            description = "ok",
            isGood = true,
            value = Json.fromDoubleOrNull(42)
          ),
          Report.ByRow(
            rowId = 2,
            col = "b",
            description = "not ok",
            isGood = false,
            value = Json.fromString("hey there")
          )
        ),
        featureReports = Map(
          "a" -> Seq(Report.ByFeature("ok", true), Report.ByFeature("really-good", true)),
          "b" -> Seq(Report.ByFeature("not-ok", false), Report.ByFeature("really-not-good", false))
        )
      )
      val res = ReportRepository.create(report)
      assertM(res)(Assertion.equalTo(report))
    },
    testM("should return specific report") {
      val report = Report(
        pluginId = "test-plugin",
        modelName = "model",
        modelVersion = 2,
        file = "s3://test/specific.csv",
        rowReports = Seq(
          Report.ByRow(
            rowId = 1,
            col = "a",
            description = "ok",
            isGood = true,
            value = Json.fromDoubleOrNull(42)
          ),
          Report.ByRow(
            rowId = 2,
            col = "b",
            description = "not ok",
            isGood = false,
            value = Json.fromString("hey there")
          )
        ),
        featureReports = Map(
          "a" -> Seq(Report.ByFeature("ok", true), Report.ByFeature("really-good", true)),
          "b" -> Seq(Report.ByFeature("not-ok", false), Report.ByFeature("really-not-good", false))
        )
      )
      val res = ReportRepository.create(report) *> ReportRepository.get("model", 2, "s3://test/specific.csv").runCollect
      assertM(res)(Assertion.equalTo(Chunk(report)))
    },
    testM("should return all inference files for a model") {
      val reports = Chunk(
        Report(
          pluginId = "test-plugin-1",
          modelName = "model",
          modelVersion = 3,
          file = "s3://test/specific1.csv",
          rowReports = Seq(
            Report.ByRow(
              rowId = 1,
              col = "a",
              description = "ok",
              isGood = true,
              value = Json.fromDoubleOrNull(42)
            ),
            Report.ByRow(
              rowId = 2,
              col = "b",
              description = "not ok",
              isGood = false,
              value = Json.fromString("hey there")
            )
          ),
          featureReports = Map(
            "a" -> Seq(Report.ByFeature("ok", true), Report.ByFeature("really-good", true)),
            "b" -> Seq(Report.ByFeature("not-ok", false), Report.ByFeature("really-not-good", false))
          )
        ),
        Report(
          pluginId = "test-plugin-2",
          modelName = "model",
          modelVersion = 3,
          file = "s3://test/specific1.csv",
          rowReports = Seq(
            Report.ByRow(
              rowId = 1,
              col = "a",
              description = "ok",
              isGood = true,
              value = Json.fromDoubleOrNull(42)
            ),
            Report.ByRow(
              rowId = 2,
              col = "b",
              description = "not ok",
              isGood = false,
              value = Json.fromString("hey there")
            )
          ),
          featureReports = Map(
            "a" -> Seq(Report.ByFeature("ok", true), Report.ByFeature("really-good", true)),
            "b" -> Seq(Report.ByFeature("not-ok", false), Report.ByFeature("really-not-good", false))
          )
        ),
        Report(
          pluginId = "test-plugin-1",
          modelName = "model",
          modelVersion = 3,
          file = "s3://test/specific2.csv",
          rowReports = Seq(
            Report.ByRow(
              rowId = 1,
              col = "a",
              description = "ok",
              isGood = true,
              value = Json.fromDoubleOrNull(42)
            ),
            Report.ByRow(
              rowId = 2,
              col = "b",
              description = "not ok",
              isGood = false,
              value = Json.fromString("hey there")
            )
          ),
          featureReports = Map(
            "a" -> Seq(Report.ByFeature("ok", true), Report.ByFeature("really-good", true)),
            "b" -> Seq(Report.ByFeature("not-ok", false), Report.ByFeature("really-not-good", false))
          )
        )
      )
      val expected = Set("s3://test/specific1.csv", "s3://test/specific2.csv")
      val prog =
        ZIO.foreach_(reports)(ReportRepository.create) *> ReportRepository.peekForModelVersion("model", 3).runCollect
      assertM(prog.map(_.toSet))(Assertion.equalTo(expected))
    }
  ) @@ MigrationAspects.migrate()).provideCustomLayerShared(Deps.testLayer)
}
