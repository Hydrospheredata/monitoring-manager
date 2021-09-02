package io.hydrosphere.monitoring.manager.domain.plugin

import io.circe.generic.JsonCodec
import io.getquill.Embedded
import io.hydrosphere.monitoring.manager.util.UriUtil
import sttp.client3._
import sttp.client3.asynchttpclient.zio._
import sttp.client3.circe._
import zio.ZIO

/** Information that plugin exposes to the manager
  */
@JsonCodec()
final case class PluginInfo(
    iconUrl: String,
    routePath: String,
    ngModuleName: String,
    remoteEntry: String,
    remoteName: String,
    exposedModule: String
) extends Embedded //NB(bulat): quill will embed this case class into Plugin table

object PluginInfo {
  final val PluginInfoUri = uri"/plugininfo.json"

  def getPluginInfo(baseUri: sttp.model.Uri): ZIO[SttpClient, Throwable, PluginInfo] = {
    val fullUri = UriUtil.combine(baseUri, PluginInfoUri)

    val request = basicRequest.get(fullUri).response(asJson[PluginInfo])
    for {
      resp       <- send(request)
      pluginInfo <- ZIO.fromEither(resp.body)
    } yield pluginInfo
  }

}
