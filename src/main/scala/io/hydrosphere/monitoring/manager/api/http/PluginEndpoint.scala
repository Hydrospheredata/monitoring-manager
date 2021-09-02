package io.hydrosphere.monitoring.manager.api.http

import io.circe.generic.JsonCodec
import io.hydrosphere.monitoring.manager.domain.clouddriver.{CloudDriver, CloudInstance}
import io.hydrosphere.monitoring.manager.domain.plugin._
import sttp.client3.asynchttpclient.zio.SttpClient
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import sttp.tapir.EndpointIO
import sttp.tapir.ztapir._
import zio._

@JsonCodec()
case class CreatePluginRequest(
    name: String,
    image: String,
    depConfigName: String,
    description: String
)

case class PluginEndpoint(
    pluginRepo: Has[PluginRepository],
    cloudDriver: Has[CloudDriver],
    backend: SttpClient,
    zenv: ZEnv
) {
  val throwableBody: EndpointIO.Body[String, Throwable] =
    plainBody[String].map[Throwable]((x: String) => new Error(x))(x => x.getMessage)

  val pluginAdd =
    endpoint
      .name("pluginAdd")
      .description("Register a new plugin")
      .in("plugin")
      .post
      .in(jsonBody[CreatePluginRequest])
      .out(jsonBody[Plugin])
      .errorOut(throwableBody)
      .serverLogic[Task](request => PluginService.register(request).provide(pluginRepo).either)

  val pluginList = endpoint
    .name("pluginList")
    .description("List all registered plugins")
    .in("plugin")
    .get
    .out(jsonBody[Seq[Plugin]])
    .errorOut(throwableBody)
    .serverLogic[Task](_ =>
      PluginRepository
        .list()
        .provide(pluginRepo)
        .either
    )

  val pluginStart = endpoint
    .name("pluginStart")
    .description("Start a plugin")
    .in("plugin" / path[String])
    .post
    .out(jsonBody[Plugin])
    .errorOut(throwableBody)
    .serverLogic[Task] { name =>
      PluginService
        .activate(name)
        .provide(pluginRepo ++ cloudDriver ++ backend ++ zenv)
        .either
    }

  val endpoints = List(pluginAdd, pluginList, pluginStart)
  val options   = ZioHttpServerOptions.default
  val routes = ZioHttpInterpreter()
    .toHttp(endpoints) //.toHttp(endpoints).toRoutes
}

object PluginEndpoint {
  def layer = (for {
    pluginRepo <- ZIO.environment[Has[PluginRepository]]
    cd         <- ZIO.environment[Has[CloudDriver]]
    zioSttp    <- ZIO.environment[SttpClient]
    zEnv       <- ZIO.environment[ZEnv]
  } yield PluginEndpoint(pluginRepo, cd, zioSttp, zEnv)).toLayer
}
