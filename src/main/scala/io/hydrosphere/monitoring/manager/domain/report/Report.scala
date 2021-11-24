package io.hydrosphere.monitoring.manager.domain.report

import io.circe.generic.JsonCodec
import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}
import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId
import io.hydrosphere.monitoring.manager.domain.report.Report.{BatchStats, FeatureReports}
import io.hydrosphere.monitoring.manager.util.{QuillJson, URI}
import monitoring_manager.monitoring_manager.{AnalyzedAck, BatchStatistics, FRRow}

import java.time.Instant

@JsonCodec
case class Report(
    pluginId: PluginId,
    modelName: ModelName,
    modelVersion: ModelVersion,
    file: URI,
    fileModifiedAt: Instant,
    featureReports: FeatureReports,
    batchStats: Option[BatchStats]
)

object Report {
  @JsonCodec
  case class BatchStats(
      susRatio: Double,
      susVerdict: String,
      failRatio: Double
  )

  object BatchStats {
    def fromProto(proto: BatchStatistics) = BatchStats(
      susRatio = proto.susRatio,
      susVerdict = proto.susVerdict,
      failRatio = proto.failRatio
    )
  }

  implicit val bsDecoder = QuillJson.jsonDecoder[BatchStats]
  implicit val bsEncoder = QuillJson.jsonEncoder[BatchStats]

  type FeatureReports = Map[String, Seq[ByFeature]]

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

  implicit val frDecoder = QuillJson.jsonDecoder[FeatureReports]
  implicit val frEncoder = QuillJson.jsonEncoder[FeatureReports]

  def fromPluginAck(pluginId: String, ack: AnalyzedAck) =
    for {
      fileObj   <- ack.inferenceDataObj.toRight(".inferenceDataObj field is empty")
      fileKey   <- URI.parse(fileObj.key)
      timestamp <- fileObj.lastModifiedAt.toRight(".lastModifiedAt field is empty")
    } yield Report(
      pluginId = pluginId,
      modelName = ack.modelName,
      modelVersion = ack.modelVersion,
      file = fileKey,
      fileModifiedAt = timestamp.asJavaInstant,
      featureReports = ack.featureReports.map { case (key, proto) => key -> proto.rows.map(ByFeature.fromProto) },
      batchStats = ack.batchStats.map(BatchStats.fromProto)
    )
}
