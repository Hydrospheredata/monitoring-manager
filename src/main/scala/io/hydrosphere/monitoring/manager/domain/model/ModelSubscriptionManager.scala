package io.hydrosphere.monitoring.manager.domain.model

import io.hydrosphere.monitoring.manager.domain.plugin.Plugin.PluginId
import zio.stream.ZStream
import zio._

final case class ModelSubscriptionManager(modelRepository: ModelRepository, hub: zio.Hub[Model]) {
  def subscribe(pluginId: PluginId) =
    modelRepository.all().concat(ZStream.fromHub(hub))
}

object ModelSubscriptionManager {
  val layer = (ModelSubscriptionManager.apply _).toLayer
}
