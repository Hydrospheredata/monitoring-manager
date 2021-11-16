package io.hydrosphere.monitoring.manager.domain.report

import io.circe.Json
import io.circe.generic.JsonCodec
import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}
import io.hydrosphere.monitoring.manager.domain.model.ModelService.ModelNotFound
import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId
import io.hydrosphere.monitoring.manager.domain.report.Report.ByFeature
import io.hydrosphere.monitoring.manager.domain.report.ReportErrors.ReportNotFound
import monitoring_manager.monitoring_manager.RowReport.Value
import monitoring_manager.monitoring_manager.{AnalyzedAck, FRRow, RowReport}
import zio._
import zio.stream.ZStream

@JsonCodec
case class Report(
    pluginId: PluginId,
    modelName: ModelName,
    modelVersion: ModelVersion,
    file: String,
    rowReports: Seq[Report.ByRow],
    featureReports: Map[String, Seq[ByFeature]]
)

object Report {
  @JsonCodec
  case class ByRow(
      rowId: Long,
      col: String,
      description: String,
      isGood: Boolean = false,
      value: Json
  )
  object ByRow {
    def fromProto(proto: RowReport) = ByRow(
      rowId = proto.rowId,
      col = proto.col,
      description = proto.description,
      isGood = proto.isGood,
      value = proto.value match {
        case Value.Empty            => Json.Null
        case Value.IntVal(value)    => Json.fromLong(value)
        case Value.StringVal(value) => Json.fromString(value)
        case Value.DoubleVal(value) => Json.fromDoubleOrString(value)
        case Value.BoolVal(value)   => Json.fromBoolean(value)
      }
    )
  }

  @JsonCodec
  case class ByFeature(
      description: String,
      isGood: Boolean
  )
  object ByFeature {
    def fromProto(proto: FRRow) = ByFeature(
      description = proto.description,
      isGood = proto.isGood
    )
  }

  def fromPluginAck(ack: AnalyzedAck) =
    Report(
      pluginId = ack.pluginId,
      modelName = ack.modelName,
      modelVersion = ack.modelVersion,
      file = ack.inferenceDataObj,
      rowReports = ack.rowReports.map(ByRow.fromProto),
      featureReports = ack.featureReports.map { case (key, proto) => key -> proto.rows.map(ByFeature.fromProto) }
    )
}

trait ReportRepository {
  def create(report: Report): ZIO[Any, Throwable, Report]

  def get(modelName: ModelName, modelVersion: ModelVersion, inferenceFile: String): ZIO[Any, ReportNotFound, Report]

  /** Returns stream of inference files with reports for a model
    */
  def peekForModelVersion(modelName: ModelName, modelVersion: ModelVersion): ZStream[Any, ModelNotFound, String]
}

object ReportErrors {
  case class ReportNotFound(modelName: ModelName, modelVersion: ModelVersion, inferenceFile: String)
      extends Error(s"Can't find report for $inferenceFile ($modelName:$modelVersion)")
}

object ReportService {}
