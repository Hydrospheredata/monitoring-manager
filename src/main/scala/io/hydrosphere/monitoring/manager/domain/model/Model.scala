package io.hydrosphere.monitoring.manager.domain.model

import io.circe.generic.JsonCodec
import io.hydrosphere.monitoring.manager.domain.contract.Signature
import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}
import io.hydrosphere.monitoring.manager.util.URI

@JsonCodec
final case class Model(
    name: ModelName,
    version: ModelVersion,
    signature: Signature,
    metadata: Map[String, String],
    trainingDataPrefix: Option[URI], // e.g. s3://bucket1/test/prefix
    inferenceDataPrefix: Option[URI] // e.g. s3://bucket131211/test/prefix
)

object Model {
  type ModelName    = String
  type ModelVersion = Long
}
