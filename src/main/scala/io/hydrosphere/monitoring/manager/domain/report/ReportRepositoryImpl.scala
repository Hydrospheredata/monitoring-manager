package io.hydrosphere.monitoring.manager.domain.report

import io.getquill.context.ZioJdbc._
import io.hydrosphere.monitoring.manager.db.{CloseableDataSource, DatabaseContext}
import io.hydrosphere.monitoring.manager.domain.data.S3Ref
import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}
import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId
import io.hydrosphere.monitoring.manager.util.URI
import zio.stream.ZStream
import zio.{ZIO, _}

import java.time.OffsetDateTime

case class ReportRepositoryImpl(
    dataSource: CloseableDataSource,
    ctx: DatabaseContext
) extends ReportRepository {
  import ctx._

  final private val hasDS = Has(dataSource)

  override def create(report: Report): ZIO[Any, Throwable, Report] =
    ctx.run(query[Report].insert(lift(report))).onDS.provide(hasDS).as(report)

  override def get(
      modelName: ModelName,
      modelVersion: ModelVersion,
      inferenceFile: URI
  ): ZStream[Any, Throwable, Report] = ctx
    .stream(
      query[Report].filter(x =>
        x.modelName == lift(modelName) &&
          x.modelVersion == lift(modelVersion) &&
          x.file == lift(inferenceFile)
      )
    )
    .provideLayer(DataSourceLayer.live)
    .provide(hasDS)

  /** Returns stream of inference files with reports for a model
    */
  override def peekForModelVersion(
      modelName: ModelName,
      modelVersion: ModelVersion
  ): ZStream[Any, Throwable, URI] = ctx
    .stream(
      query[Report]
        .filter(x =>
          x.modelName == lift(modelName) &&
            x.modelVersion == lift(modelVersion)
        )
        .map(_.file)
        .distinct
    )
    .provideLayer(DataSourceLayer.live)
    .provide(hasDS)

  override def exists(pluginId: PluginId, s3Ref: S3Ref): ZIO[Any, Throwable, Boolean] = ctx
    .run(
      query[Report]
        .filter(a =>
          a.pluginId == lift(pluginId) &&
            a.file == lift(s3Ref.fullPath) &&
            a.fileModifiedAt == lift(s3Ref.lastModified)
        )
    )
    .map(_.nonEmpty)
    .onDS
    .provide(hasDS)
}

object ReportRepositoryImpl {
  val layer: URLayer[Has[CloseableDataSource] with Has[DatabaseContext], Has[ReportRepository]] =
    (ReportRepositoryImpl.apply _).toLayer
}
