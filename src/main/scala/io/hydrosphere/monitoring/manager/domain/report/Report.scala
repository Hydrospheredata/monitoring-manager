package io.hydrosphere.monitoring.manager.domain.report

import io.circe.Json
import io.circe.generic.JsonCodec
import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}
import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId
import io.hydrosphere.monitoring.manager.domain.report.Report.{ByFeature, FeatureReports, RowReports}
import io.hydrosphere.monitoring.manager.util.QuillJson
import monitoring_manager.monitoring_manager.RowReport.Value
import monitoring_manager.monitoring_manager.{AnalyzedAck, FRRow, RowReport}

@JsonCodec
case class Report(
    pluginId: PluginId,
    modelName: ModelName,
    modelVersion: ModelVersion,
    file: String,
    rowReports: RowReports,
    featureReports: FeatureReports
)

object Report {
  type RowReports = Seq[Report.ByRow]
  implicit val rrDecoder = QuillJson.jsonDecoder[RowReports]
  implicit val rrEncoder = QuillJson.jsonEncoder[RowReports]

  type FeatureReports = Map[String, Seq[ByFeature]]
  implicit val frDecoder = QuillJson.jsonDecoder[FeatureReports]
  implicit val frEncoder = QuillJson.jsonEncoder[FeatureReports]

  @JsonCodec
  case class ByRow(
      rowId: Long,
      col: String,
      description: String,
      isGood: Boolean,
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

  def fromPluginAck(pluginId: String, ack: AnalyzedAck) =
    Report(
      pluginId = pluginId,
      modelName = ack.modelName,
      modelVersion = ack.modelVersion,
      file = ack.inferenceDataObj,
      rowReports = ack.rowReports.map(ByRow.fromProto),
      featureReports = ack.featureReports.map { case (key, proto) => key -> proto.rows.map(ByFeature.fromProto) }
    )
}
