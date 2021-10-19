package io.hydrosphere.monitoring.manager.domain.plugin

import enumeratum._
import io.circe.generic.JsonCodec
import io.getquill.Embedded
import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId

/** Runtme information after Plugin was started and exposed info as URL
  */
@JsonCodec()
final case class Plugin(
    name: PluginId,
    description: String,
    pluginInfo: Option[PluginInfo]
)

object Plugin {
  type PluginId = String
  sealed trait Status extends EnumEntry
  final case object Status extends Enum[Status] with CirceEnum[Status] with QuillEnum[Status] {
    case object Inactive extends Status
    case object Active   extends Status

    override def values: IndexedSeq[Status] = findValues
  }
}
