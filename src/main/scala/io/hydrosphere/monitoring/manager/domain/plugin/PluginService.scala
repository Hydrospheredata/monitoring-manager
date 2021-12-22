package io.hydrosphere.monitoring.manager.domain.plugin

import io.hydrosphere.monitoring.manager.util.URI
import io.hydrosphere.monitoring.manager.{EndpointConfig, ProxyConfig}
import zio.{Has, ZIO}

object PluginService {
  case class PluginAlreadyExistsError(pluginName: String) extends Error(s"Plugin $pluginName already exists")

  case class PluginNotFoundError(pluginName: String) extends Error(s"Can't find plugin $pluginName")

  case class PluginIncompleteError(plugin: Plugin, fields: String*)
      extends Error(s"Plugin ${plugin.name} doesn't have [${fields.mkString(",")}] fields")

  /** Create new Plugin instance and add it to the persistence storage. Updates plugin if there is already one.
    *
    * @param pluginRequest
    *   @return
    */
  def register(
      plugin: Plugin
  ): ZIO[Has[PluginRepository] with Has[ProxyConfig], Throwable, Plugin] =
    for {
      config <- ZIO.service[ProxyConfig]
      newPlugin = resolveRemoteEntry(plugin, config.managerProxyUri)
      exPlugin <- PluginRepository.get(plugin.name)
      result <- exPlugin match {
        case Some(_) =>
          PluginRepository.update(newPlugin)
        case None =>
          PluginRepository.insert(newPlugin)
      }
    } yield result

  // <manager-addr>/plugin-proxy/<name>/static/remoteEntry.js -> <plugin-addr>/static/remoteEntry.js
  final val REMOTE_ENTRY_SEGMENTS = List("static", "remoteEntry.js")
  //// CALCULATED!!! MUST RESOLVE TO: ABSOLUTE URI <plugin-addr>/static/remoteEntry.js
  def resolveRemoteEntry(plugin: Plugin, managerUri: URI) = {

    val remoteEntryUri = URI(
      managerUri.u
        .addPath("api", "v1", "plugin-proxy") // NB(bulat): endpoint URL for proxy path
        .addPath(plugin.name)
        .addPath(REMOTE_ENTRY_SEGMENTS)
    )
    val newPInfo = plugin.pluginInfo.map(_.copy(remoteEntry = Some(remoteEntryUri)))
    plugin.copy(pluginInfo = newPInfo)
  }
}
