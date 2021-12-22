package io.hydrosphere.monitoring.manager.api.http

import io.hydrosphere.monitoring.manager.domain.plugin._
import io.hydrosphere.monitoring.manager.{EndpointConfig, ProxyConfig}
import zio._

case class PluginEndpoint(
    pluginRepo: PluginRepository,
    proxyConfig: ProxyConfig
) extends GenericEndpoint {

  val pluginAdd = PluginEndpoint.pluginAddDesc
    .serverLogic[Task](request => PluginService.register(request).provide(Has(pluginRepo) ++ Has(proxyConfig)).either)

  val pluginList = PluginEndpoint.pluginListDesc
    .serverLogic[Task](_ =>
      pluginRepo
        .all()
        .runCollect
        .either
    )

  val serverEndpoints = List(pluginAdd, pluginList)
}

object PluginEndpoint extends GenericEndpoint {
  val pluginEndpoint = v1Endpoint
    .in("plugin")
    .tag("Plugin")

  val pluginAddDesc =
    pluginEndpoint
      .name("pluginAdd")
      .description("Register a new plugin")
      .post
      .in(jsonBody[Plugin])
      .out(jsonBody[Plugin])
      .errorOut(throwableBody)

  val pluginListDesc = pluginEndpoint
    .name("pluginList")
    .description("List all registered plugins")
    .get
    .out(jsonBody[Seq[Plugin]])
    .errorOut(throwableBody)

  val endpoints = List(pluginListDesc, pluginAddDesc)

  def layer = (PluginEndpoint.apply _).toLayer
}
