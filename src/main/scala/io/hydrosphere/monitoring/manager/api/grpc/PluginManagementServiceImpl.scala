package io.hydrosphere.monitoring.manager.api.grpc

import io.grpc.Status
import io.hydrosphere.monitoring.manager.api.grpc.PluginManagementServiceImpl.requestToPlugin
import io.hydrosphere.monitoring.manager.domain.plugin._
import io.hydrosphere.monitoring.manager.util.URI
import io.hydrosphere.monitoring.manager.EndpointConfig
import monitoring_manager.monitoring_manager.{RegisterPluginRequest, RegisterPluginResponse}
import monitoring_manager.monitoring_manager.ZioMonitoringManager.PluginManagementService
import zio._
import zio.logging.Logger

final case class PluginManagementServiceImpl(
    pluginRepository: PluginRepository,
    endpointConfig: EndpointConfig,
    log: Logger[String]
) extends PluginManagementService {
  override def registerPlugin(request: RegisterPluginRequest) =
    for {
      plugin <- ZIO
        .fromEither(requestToPlugin(request))
        .mapError(Status.INVALID_ARGUMENT.withDescription)
      _ <- PluginService
        .register(plugin)
        .tapError(err => log.throwable("Error during plugin registration", err))
        .mapError(Status.INTERNAL.withCause)
        .provide(Has(pluginRepository) ++ Has(endpointConfig))
    } yield RegisterPluginResponse()
}

object PluginManagementServiceImpl {
  val layer: URLayer[Has[PluginRepository] with Has[EndpointConfig] with Has[Logger[String]], Has[
    PluginManagementService
  ]] =
    (PluginManagementServiceImpl.apply _).toLayer

  def requestToPlugin(request: RegisterPluginRequest) =
    for {
      addr <- URI.parse(request.addr)
    } yield Plugin(
      name = request.pluginId,
      description = request.description,
      pluginInfo = Some(
        PluginInfo(
          routePath = request.routePath,
          ngModuleName = request.ngModuleName,
          remoteName = request.remoteName,
          exposedModule = request.exposedModule,
          addr = addr,
          remoteEntry = None
        )
      )
    )
}
