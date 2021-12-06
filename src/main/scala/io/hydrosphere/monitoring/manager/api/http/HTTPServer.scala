package io.hydrosphere.monitoring.manager.api.http

import io.hydrosphere.monitoring.manager.EndpointConfig
import io.hydrosphere.monitoring.manager.api.http.HTTPServer.routes
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{CORS, CORSConfig, RHttpApp}
import zhttp.service.{EventLoopGroup, Server}
import zhttp.service.server.ServerChannelFactory
import zio.{Has, ZIO}
import zio.logging.log

object HTTPServer {
  def routes = for {
    pluginEndpoint <- ZIO.service[PluginEndpoint]
    modelEndpoint  <- ZIO.service[ModelEndpoint]
    proxyEndpoint  <- ZIO.service[PluginProxyEndpoint]
    reportEndpoint <- ZIO.service[ReportEndpoint]
    corsConfig = CORSConfig(
      anyOrigin = true,
      anyMethod = true,
      allowCredentials = true
    )
    allRoutes =
      pluginEndpoint.serverEndpoints ++ modelEndpoint.serverEndpoints ++ proxyEndpoint.serverEndpoints ++ reportEndpoint.serverEndpoints
    compiled = ZioHttpInterpreter().toHttp(allRoutes)
    routes   = CORS(compiled, corsConfig)
  } yield routes

  def start[R <: Has[_]](port: Int, http: RHttpApp[R]): ZIO[R, Throwable, Nothing] =
    (Server.port(port) ++ Server.app(http)).make.useForever
      .provideSomeLayer[R](EventLoopGroup.auto(0) ++ ServerChannelFactory.auto)

  val make = for {
    config <- ZIO.service[EndpointConfig].toManaged_
    routes <- routes.toManaged_
    server <- (Server.port(config.httpPort) ++ Server.app(routes) ++ Server.maxRequestSize(
      config.httpMaxRequestSize
    )).make
  } yield server

  def start =
    for {
      config <- ZIO.service[EndpointConfig]
      routes <- routes
      _      <- log.info(s"Starting HTTP server at ${config.httpPort} port")
      _ <- (Server.simpleLeakDetection ++
        Server.port(config.httpPort) ++
        Server.app(routes) ++
        Server.maxRequestSize(config.httpMaxRequestSize)).make.useForever
        .provideSomeLayer(EventLoopGroup.auto(0) ++ ServerChannelFactory.auto)
    } yield ()
}
