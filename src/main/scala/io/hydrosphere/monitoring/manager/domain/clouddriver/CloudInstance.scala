package io.hydrosphere.monitoring.manager.domain.clouddriver

import io.circe._
import io.circe.generic.semiauto._
import sttp.model.Uri
import sttp.tapir.Schema
import sttp.tapir.generic.auto._

final case class CloudInstance(
    name: String,
    image: String,
    deploymentConfigName: String,
    uri: Uri
)

object CloudInstance {
  implicit val uriEncoder: Encoder[Uri] = Encoder.encodeString.contramap(x => x.toString())
  implicit val uriDecoder: Decoder[Uri] = Decoder.decodeString.emap(Uri.parse)
  implicit val ciCodec: Codec.AsObject[CloudInstance] = deriveCodec

  implicit val uriSchema: Schema[Uri] =
    Schema.schemaForString.map(Uri.parse(_).toOption)(_.toString())

  implicit val ciSchema = implicitly[Schema[CloudInstance]]
}
