package io.hydrosphere.monitoring.manager.domain.plugin

import enumeratum._
import io.circe.generic.JsonCodec

/** Runtme information after Plugin was started and exposed info as URL
  */
@JsonCodec()
final case class Plugin(
    name: String,
    image: String,
    depConfigName: String,
    description: String,
    status: Plugin.Status,
    pluginInfo: Option[PluginInfo]
)

object Plugin {
  sealed trait Status extends EnumEntry
  final case object Status extends Enum[Status] with CirceEnum[Status] with QuillEnum[Status] {
    case object Inactive extends Status
    case object Active   extends Status

    override def values: IndexedSeq[Status] = findValues
  }
}
