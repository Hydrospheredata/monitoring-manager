package io.hydrosphere.monitoring.manager.api.http

import io.hydrosphere.monitoring.manager.EndpointConfig
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{CORS, CORSConfig}
import zhttp.service.Server
import zio.{Has, ZIO}
import zio.logging.log

object HTTPServer {
  def routes = for {
    pluginEndpoint <- ZIO.service[PluginEndpoint]
    modelEndpoint  <- ZIO.service[ModelEndpoint]
    proxyEndpoint  <- ZIO.service[PluginProxyEndpoint]
    corsConfig = CORSConfig(
      anyOrigin = true,
      anyMethod = true,
      allowCredentials = true
    )
    allRoutes =
      pluginEndpoint.serverEndpoints ++ modelEndpoint.serverEndpoints ++ proxyEndpoint.serverEndpoints
    compiled = ZioHttpInterpreter().toHttp(allRoutes)
    routes   = CORS(compiled, corsConfig)
  } yield routes

  def start =
    for {
      routes   <- routes
      httpPort <- ZIO.access[Has[EndpointConfig]](_.get.httpPort)
      _        <- log.info(s"Starting HTTP server at $httpPort port")
      _        <- Server.start(httpPort, routes)
    } yield ()
}
