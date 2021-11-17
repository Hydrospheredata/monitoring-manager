package io.hydrosphere.monitoring.manager.domain.report

import io.getquill.context.ZioJdbc._
import io.hydrosphere.monitoring.manager.db.{CloseableDataSource, DatabaseContext}
import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}
import zio.{ZIO, _}
import zio.stream.ZStream

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
      inferenceFile: String
  ): ZStream[Any, Throwable, Report] = ctx
    .stream(
      query[Report].filter(x =>
        x.modelName == lift(modelName) && x.modelVersion == lift(modelVersion) && x.file == lift(inferenceFile)
      )
    )
    .provideLayer(DataSourceLayer.live)
    .provide(hasDS)

  /** Returns stream of inference files with reports for a model
    */
  override def peekForModelVersion(
      modelName: ModelName,
      modelVersion: ModelVersion
  ): ZStream[Any, Throwable, String] = ctx
    .stream(
      query[Report]
        .filter(x => x.modelName == lift(modelName) && x.modelVersion == lift(modelVersion))
        .map(_.file)
        .distinct
    )
    .provideLayer(DataSourceLayer.live)
    .provide(hasDS)
}

object ReportRepositoryImpl {
  val layer: URLayer[Has[CloseableDataSource] with Has[DatabaseContext], Has[ReportRepository]] =
    (ReportRepositoryImpl.apply _).toLayer
}
