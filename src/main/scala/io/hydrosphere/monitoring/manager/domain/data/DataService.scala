package io.hydrosphere.monitoring.manager.domain.data

import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId
import zio.stream.ZStream

object DataService {

  case class NoInferenceData(pluginId: PluginId) extends Error

  def subscibeToInferenceData(
      pluginId: PluginId
  ) =
    for {
      subManager <- ZStream.service[InferenceSubscriptionService]
      hub        <- ZStream.fromEffect(subManager.subscribe(pluginId))
      data       <- ZStream.fromHub(hub)
    } yield data
}
