package io.hydrosphere.monitoring.manager.domain.plugin

import io.circe.generic.JsonCodec
import io.getquill.Embedded
import io.hydrosphere.monitoring.manager.util.URI

/** Information that plugin exposes to the manager
  */
@JsonCodec()
final case class PluginInfo(
    addr: URI, //TODO(bulat): is this the right place for plugin address?
    routePath: String,
    ngModuleName: String,
    remoteEntry: Option[URI],
    remoteName: String,
    exposedModule: String
) extends Embedded //NB(bulat): quill will embed this case class into Plugin table
