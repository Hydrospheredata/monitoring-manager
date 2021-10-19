package io.hydrosphere.monitoring.manager.api.http

import io.hydrosphere.monitoring.manager.domain.plugin._
import sttp.client3.asynchttpclient.zio.SttpClient
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import sttp.tapir.ztapir._
import zio._

case class PluginEndpoint(
    pluginRepo: Has[PluginRepository],
    backend: SttpClient,
    zenv: ZEnv
) extends GenericEndpoint {

  val pluginEndpoint = v1Endpoint
    .in("plugin")
    .tag("Plugin")

  val pluginAdd =
    pluginEndpoint
      .name("pluginAdd")
      .description("Register a new plugin")
      .post
      .in(jsonBody[Plugin])
      .out(jsonBody[Plugin])
      .errorOut(throwableBody)
      .serverLogic[Task](request => PluginService.register(request).provide(pluginRepo).either)

  val pluginList = pluginEndpoint
    .name("pluginList")
    .description("List all registered plugins")
    .get
    .out(jsonBody[Seq[Plugin]])
    .errorOut(throwableBody)
    .serverLogic[Task](_ =>
      PluginRepository
        .all()
        .runCollect
        .provide(pluginRepo)
        .either
    )

  val endpoints = List(pluginAdd, pluginList)
}

object PluginEndpoint {
  def layer = (for {
    pluginRepo <- ZIO.environment[Has[PluginRepository]]
    zioSttp    <- ZIO.environment[SttpClient]
    zEnv       <- ZIO.environment[ZEnv]
  } yield PluginEndpoint(pluginRepo, zioSttp, zEnv)).toLayer
}
