package io.hydrosphere.monitoring.manager.api.grpc

import io.grpc.Status
import io.hydrosphere.monitoring.manager.api.grpc.PluginManagementServiceImpl.requestToPlugin
import io.hydrosphere.monitoring.manager.domain.plugin._
import monitoring_manager.monitoring_manager.{RegisterPluginRequest, RegisterPluginResponse}
import monitoring_manager.monitoring_manager.ZioMonitoringManager.PluginManagementService
import zio.Has

final class PluginManagementServiceImpl(pluginRepository: PluginRepository)
    extends PluginManagementService {
  override def registerPlugin(request: RegisterPluginRequest) =
    PluginService
      .register(requestToPlugin(request))
      .as(RegisterPluginResponse())
      .provide(Has(pluginRepository))
      .mapError(x => Status.INTERNAL.withCause(x))
}

object PluginManagementServiceImpl {
  def requestToPlugin(request: RegisterPluginRequest) = Plugin(
    name = request.pluginId,
    description = request.description,
    pluginInfo = Some(
      PluginInfo(
        iconUrl = request.iconUrl,
        routePath = request.routePath,
        ngModuleName = request.ngModuleName,
        remoteEntry = request.remoteEntry,
        remoteName = request.remoteName,
        exposedModule = request.exposedModule
      )
    )
  )
}
