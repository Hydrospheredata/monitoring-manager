package io.hydrosphere.monitoring.manager.domain

import enumeratum.{CirceEnum, Enum, EnumEntry, QuillEnum}
import io.hydrosphere.monitoring.manager.util.QuillJson
import monitoring_manager.monitoring_manager.{
  ModelField,
  ModelSignature,
  TensorShape,
  DataProfileType => ProtoDataProfileType,
  DataType => ProtoDataType
}
import io.circe.generic.JsonCodec
import io.circe.generic.semiauto._

object contract {

  sealed abstract class DataType(val proto: ProtoDataType) extends EnumEntry

  object DataType extends Enum[DataType] with CirceEnum[DataType] with QuillEnum[DataType] {
    case object DT_FLOAT  extends DataType(ProtoDataType.DT_FLOAT)
    case object DT_DOUBLE extends DataType(ProtoDataType.DT_DOUBLE)
    case object DT_INT32  extends DataType(ProtoDataType.DT_INT32)
    case object DT_UINT8  extends DataType(ProtoDataType.DT_UINT8)
    case object DT_INT16  extends DataType(ProtoDataType.DT_INT16)
    case object DT_INT8   extends DataType(ProtoDataType.DT_INT8)
    case object DT_STRING extends DataType(ProtoDataType.DT_STRING)
    case object DT_INT64  extends DataType(ProtoDataType.DT_INT64)
    case object DT_BOOL   extends DataType(ProtoDataType.DT_BOOL)
    case object DT_UINT16 extends DataType(ProtoDataType.DT_UINT16)
    case object DT_HALF   extends DataType(ProtoDataType.DT_HALF)
    case object DT_UINT32 extends DataType(ProtoDataType.DT_UINT32)
    case object DT_UINT64 extends DataType(ProtoDataType.DT_UINT64)
    case object DT_ANY    extends DataType(ProtoDataType.DT_ANY)

    override def values = findValues
  }

  sealed abstract class DataProfileType(val proto: ProtoDataProfileType) extends EnumEntry

  object DataProfileType extends Enum[DataProfileType] with CirceEnum[DataProfileType] with QuillEnum[DataProfileType] {
    case object NONE        extends DataProfileType(ProtoDataProfileType.NONE)
    case object CATEGORICAL extends DataProfileType(ProtoDataProfileType.CATEGORICAL)
    case object NOMINAL     extends DataProfileType(ProtoDataProfileType.NOMINAL)
    case object ORDINAL     extends DataProfileType(ProtoDataProfileType.ORDINAL)
    case object NUMERICAL   extends DataProfileType(ProtoDataProfileType.NUMERICAL)
    case object CONTINUOUS  extends DataProfileType(ProtoDataProfileType.CONTINUOUS)
    case object INTERVAL    extends DataProfileType(ProtoDataProfileType.INTERVAL)
    case object RATIO       extends DataProfileType(ProtoDataProfileType.RATIO)
    case object IMAGE       extends DataProfileType(ProtoDataProfileType.IMAGE)
    case object VIDEO       extends DataProfileType(ProtoDataProfileType.VIDEO)
    case object AUDIO       extends DataProfileType(ProtoDataProfileType.AUDIO)
    case object TEXT        extends DataProfileType(ProtoDataProfileType.TEXT)

    override def values = findValues
  }

  case class Signature(inputs: Seq[Field], outputs: Seq[Field]) {
    def toProto = ModelSignature(inputs.map(_.toProto), outputs.map(_.toProto))
  }

  @JsonCodec
  case class Field(name: String, shape: Option[Seq[Long]], dtype: DataType, profile: DataProfileType) {
    def toProto = ModelField(
      name,
      shape.map(x => TensorShape.apply(x)),
      dtype.proto,
      profile.proto
    )
  }

  object Signature {
    implicit val codec        = deriveCodec[Signature]
    implicit val quillDecoder = QuillJson.jsonDecoder[Signature]
    implicit val quillEncoder = QuillJson.jsonEncoder[Signature]
  }
}
