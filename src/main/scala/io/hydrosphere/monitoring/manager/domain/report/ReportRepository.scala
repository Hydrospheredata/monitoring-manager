package io.hydrosphere.monitoring.manager.domain.report

import io.hydrosphere.monitoring.manager.domain.data.S3Ref
import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}
import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId
import io.hydrosphere.monitoring.manager.util.URI
import zio.ZIO
import zio.macros.accessible
import zio.stream.ZStream

@accessible
trait ReportRepository {
  def create(report: Report): ZIO[Any, Throwable, Report]

  def get(modelName: ModelName, modelVersion: ModelVersion, inferenceFile: URI): ZStream[Any, Throwable, Report]

  def exists(
      pluginId: PluginId,
      s3Ref: S3Ref
  ): ZIO[Any, Throwable, Boolean]

  /** Returns stream of inference files with reports for a model
    */
  def peekForModelVersion(modelName: ModelName, modelVersion: ModelVersion): ZStream[Any, Throwable, URI]
}
