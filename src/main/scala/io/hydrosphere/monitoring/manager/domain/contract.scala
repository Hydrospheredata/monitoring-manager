package io.hydrosphere.monitoring.manager.domain

import io.hydrosphere.monitoring.manager.util.QuillJson
import monitoring_manager.monitoring_manager.{
  DataProfileType,
  DataType,
  ModelField,
  ModelSignature,
  TensorShape
}
import io.circe.generic.JsonCodec
import io.circe.generic.semiauto._

object contract {
  case class Signature(inputs: Seq[Field], outputs: Seq[Field]) {
    def toProto = ModelSignature(inputs.map(_.toProto), outputs.map(_.toProto))
  }

  @JsonCodec
  case class Field(name: String, shape: Option[Seq[Long]], dtype: String, profile: String) {
    def toProto = ModelField(
      name,
      shape.map(x => TensorShape.apply(x)),
      DataType.fromName(dtype).getOrElse(DataType.DT_INVALID),
      DataProfileType.fromName(profile).getOrElse(DataProfileType.NONE)
    )
  }

  object Signature {
    implicit val codec        = deriveCodec[Signature]
    implicit val quillDecoder = QuillJson.jsonDecoder[Signature]
    implicit val quillEncoder = QuillJson.jsonEncoder[Signature]
  }
}
