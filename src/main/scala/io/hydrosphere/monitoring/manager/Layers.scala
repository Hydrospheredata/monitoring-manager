package io.hydrosphere.monitoring.manager

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.github.vigoo.zioaws._
import io.hydrosphere.monitoring.manager.api.http._
import io.hydrosphere.monitoring.manager.db.{DatabaseContext, FlywayClient}
import io.hydrosphere.monitoring.manager.domain.data._
import io.hydrosphere.monitoring.manager.domain.model._
import io.hydrosphere.monitoring.manager.domain.plugin.PluginRepositoryImpl
import sttp.client3.asynchttpclient.zio.{AsyncHttpClientZioBackend, SttpClient}
import zio.{Has, Layer, ULayer, ZEnv, ZHub, ZLayer}
import zio.logging.{LogAnnotation, Logger}
import zio.logging.slf4j.Slf4jLogger

/** Define all dependencies here. Construct services using ZLayers. Merge contructed ZLayers with
  * API layer. API Layer is to be initialized and executed by zio.App
  */
object Layers {
  val logger: ULayer[Has[Logger[String]]] = {
    val logFormat = "[corr-id=%s] %s"
    Slf4jLogger.make { (context, message) =>
      val correlationId =
        context.get(LogAnnotation.CorrelationId).map(_.toString).getOrElse("undefined")
      logFormat.format(correlationId, message)
    }
  }

  val s3Client =
    (netty.default ++ Config.layer) >>> core.config.configured() >>> s3.live >>> S3Client.layer

  val modelHub = ZHub.unbounded[Model].toLayer

  val db = {
    val dbLayer         = DataSourceLayer.fromPrefix(Config.databaseConfPrefix)
    val dbCtxLayer      = DatabaseContext.layer
    val flywayLayer     = (dbLayer >>> FlywayClient.layer).tap(_.get.migrate())
    val deps            = dbLayer ++ dbCtxLayer
    val pluginRepoLayer = deps >>> PluginRepositoryImpl.layer
    val modelRepoLayer  = deps ++ modelHub >>> ModelRepositoryImpl.layer
    flywayLayer ++ pluginRepoLayer ++ modelRepoLayer
  }

  val sttp: Layer[Throwable, Has[SttpClient.Service]] = AsyncHttpClientZioBackend.layer()

  val pluginEndpoint =
    (ZLayer.requires[ZEnv] ++ db ++ sttp ++ Config.layer) >>> PluginEndpoint.layer
  val modelEndpoint = db >>> ModelEndpoint.layer
  val proxyEndpoint = (sttp ++ db) >>> PluginProxyEndpoint.layer
  val api           = pluginEndpoint ++ modelEndpoint ++ proxyEndpoint

  val modelSub = (db ++ modelHub) >>> ModelSubscriptionManager.layer

  val inferenceSub = S3ObjectIndex.layer ++ s3Client >>> InferenceSubscriptionService.layer

  val all =
    Config.layer ++ logger ++ db ++ api ++ modelSub ++ inferenceSub
}
