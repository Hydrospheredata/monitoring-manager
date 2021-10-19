package io.hydrosphere.monitoring.manager.domain.plugin

import zio.{Has, ZIO}

object PluginService {
  case class PluginAlreadyExistsError(pluginName: String)
      extends Error(s"Plugin $pluginName already exists")

  case class PluginNotFoundError(pluginName: String) extends Error(s"Can't find plugin $pluginName")

  /** Create new Plugin instance and add it to the persistence storage. Updates plugin if there is
    * already one.
    *
    * @param pluginRequest
    *   @return
    */
  def register(plugin: Plugin): ZIO[Has[PluginRepository], Throwable, Plugin] =
    for {
      exPlugin <- PluginRepository.get(plugin.name)
      result <- exPlugin match {
        case Some(_) =>
          PluginRepository.update(plugin)
        case None =>
          PluginRepository.insert(plugin)
      }
    } yield result
}
