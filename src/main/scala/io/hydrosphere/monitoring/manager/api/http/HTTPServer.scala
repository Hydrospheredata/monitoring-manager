package io.hydrosphere.monitoring.manager.api.http

import io.hydrosphere.monitoring.manager.EndpointConfig
import zhttp.service.Server
import zio.{Has, ZIO}
import zio.logging.{log, Logging}

object HTTPServer {
  def start: ZIO[Logging with Has[EndpointConfig] with Has[PluginEndpoint], Throwable, Unit] =
    for {
      pluginEndpoint <- ZIO.service[PluginEndpoint]
      httpPort       <- ZIO.access[Has[EndpointConfig]](_.get.httpPort)
      _              <- log.info(s"Starting HTTP server at $httpPort port")
      _              <- Server.start(httpPort, pluginEndpoint.routes)
    } yield ()
}
