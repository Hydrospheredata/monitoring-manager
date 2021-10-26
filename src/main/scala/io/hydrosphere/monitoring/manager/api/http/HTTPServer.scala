package io.hydrosphere.monitoring.manager.api.http

import io.hydrosphere.monitoring.manager.EndpointConfig
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zhttp.service.Server
import zio.{Has, ZIO}
import zio.logging.{log, Logging}

object HTTPServer {
  def routes = for {
    pluginEndpoint <- ZIO.service[PluginEndpoint]
    modelEndpoint  <- ZIO.service[ModelEndpoint]
    proxyEndpoint  <- ZIO.service[PluginProxyEndpoint]
    routes = ZioHttpInterpreter()
      .toHttp(
        pluginEndpoint.serverEndpoints ++ modelEndpoint.serverEndpoints ++ proxyEndpoint.serverEndpoints
      )
  } yield routes

  def start =
    for {
      routes   <- routes
      httpPort <- ZIO.access[Has[EndpointConfig]](_.get.httpPort)
      _        <- log.info(s"Starting HTTP server at $httpPort port")
      _        <- Server.start(httpPort, routes)
    } yield ()
}
