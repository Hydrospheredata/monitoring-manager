package io.hydrosphere.monitoring.manager.api.http

import io.hydrosphere.monitoring.manager.domain.plugin.{PluginRepository, PluginService}
import io.hydrosphere.monitoring.manager.util.UriUtil
import sttp.client3._
import sttp.client3.asynchttpclient.zio.SttpClient
import zio._

case class PluginProxyEndpoint(sttpClient: SttpClient.Service, pluginRepository: PluginRepository)
    extends GenericEndpoint {
  val proxyRequest = PluginProxyEndpoint.proxyRequestDesc
    .serverLogic[Task] { case (method, pluginName, headers, path, queryParams, body) =>
      for {
        maybePlugin <- pluginRepository.get(pluginName)
        plugin <- maybePlugin match {
          case Some(value) => ZIO.succeed(value)
          case None        => ZIO.fail(PluginService.PluginNotFoundError(pluginName))
        }
        addr <- ZIO
          .fromOption(plugin.pluginInfo.map(_.addr.u))
          .orElseFail(PluginService.PluginIncompleteError(plugin, ".pluginInfo.addr"))
        addrWithPath = addr.addPath(path)
        fullAddr =
          UriUtil
            .queryPassthrough(queryParams)
            .foldLeft(addrWithPath) { case (addr, q) =>
              addr.addQuerySegment(q)
            }
        req = basicRequest
          .method(method, fullAddr)
          .headers(headers: _*)
          .body(body)
        resp <- sttpClient.send(req)
      } yield resp.body.map(b => (resp.code, resp.headers.toList, b))
    }

  val serverEndpoints = List(proxyRequest)
}

object PluginProxyEndpoint extends GenericEndpoint {
  val layer = (PluginProxyEndpoint.apply _).toLayer

  val proxyEndpoint = v1Endpoint
    .in("plugin-proxy")
    .tag("Plugin proxy")

  val proxyRequestDesc = proxyEndpoint
    .name("proxyRequest")
    .in(extractFromRequest(req => req.method))
    .in(path[String]("pluginName"))
    .in(headers)
    .in(paths)
    .in(queryParams)
    .in(byteArrayBody)
    .out(statusCode)
    .out(headers)
    .out(stringBody)
    .errorOut(stringBody)

  val endpoints = List(proxyRequestDesc)
}
