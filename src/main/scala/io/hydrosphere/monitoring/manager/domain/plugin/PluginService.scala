package io.hydrosphere.monitoring.manager.domain.plugin

import io.hydrosphere.monitoring.manager.api.http.CreatePluginRequest
import io.hydrosphere.monitoring.manager.domain.clouddriver.CloudDriver
import zio.{Has, ZIO}

object PluginService {
  case class PluginAlreadyExistsError(pluginName: String)
      extends Error(s"Plugin $pluginName already exists")

  case class PluginNotFoundError(pluginName: String) extends Error(s"Can't find plugin $pluginName")

  /** Create new Plugin instance and add it to the persistence storage. Fails if there is a plugin
    * with the same name.
    *
    * @param pluginRequest
    *   @return
    */
  def register(pluginRequest: CreatePluginRequest): ZIO[Has[PluginRepository], Throwable, Plugin] =
    for {
      exPlugin <- PluginRepository.get(pluginRequest.name)
      _ <- exPlugin match {
        case Some(value) => ZIO.fail(PluginAlreadyExistsError(value.name))
        case None        => ZIO.succeed(())
      }
      newPlugin = Plugin(
        pluginRequest.name,
        pluginRequest.image,
        pluginRequest.depConfigName,
        pluginRequest.description,
        Plugin.Status.Inactive,
        pluginInfo = None
      )
      result <- PluginRepository.insert(newPlugin)
    } yield result

  /** Creates an instance for the plugin and returns Service object that describes it.
    *
    * @param name
    *   @return
    */
  def activate(name: String) =
    for {
      plugin <- PluginRepository
        .get(name)
        .flatMap {
          case Some(value) => ZIO.succeed(value)
          case None        => ZIO.fail(PluginNotFoundError(name))
        }
      service    <- CloudDriver.createService(plugin.name, plugin.image, plugin.depConfigName)
      pluginInfo <- PluginInfo.getPluginInfo(service.uri)
      updatedPlugin = plugin.copy(pluginInfo = Some(pluginInfo), status = Plugin.Status.Active)
      _ <- PluginRepository.update(updatedPlugin)
    } yield updatedPlugin
}
