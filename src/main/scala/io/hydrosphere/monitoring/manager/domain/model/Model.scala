package io.hydrosphere.monitoring.manager.domain.model

import io.circe.generic.JsonCodec
import io.hydrosphere.monitoring.manager.domain.contract.Signature
import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}
import sttp.model.Uri
import io.circe._
import sttp.tapir.Schema

case class URI(u: Uri) extends AnyVal

object URI {
  implicit val uriEncoder: Encoder[URI] =
    Encoder.encodeString.contramap[Uri](_.toString()).contramap[URI](_.u)
  implicit val uriDecoder: Decoder[URI] = Decoder.decodeString.emap[Uri](Uri.parse).map(URI.apply)

  implicit val schema =
    Schema.schemaForString.map(str => Uri.parse(str).map(URI.apply).toOption)(_.u.toString())
}

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
