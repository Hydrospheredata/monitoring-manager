package io.hydrosphere.monitoring.manager

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.github.vigoo.zioaws._
import io.hydrosphere.monitoring.manager.api.http._
import io.hydrosphere.monitoring.manager.db.{DatabaseContext, FlywayClient}
import io.hydrosphere.monitoring.manager.domain.data._
import io.hydrosphere.monitoring.manager.domain.metrics.sender.MetricSender
import io.hydrosphere.monitoring.manager.domain.model._
import io.hydrosphere.monitoring.manager.domain.plugin.{PluginRepository, PluginRepositoryImpl}
import io.hydrosphere.monitoring.manager.domain.report.{ReportRepository, ReportRepositoryImpl}
import sttp.client3.asynchttpclient.zio.{AsyncHttpClientZioBackend, SttpClient}
import zio.blocking.Blocking
import zio.logging.slf4j.Slf4jLogger
import zio.logging.{LogAnnotation, Logger}
import zio.random.Random
import zio.{Has, Layer, ULayer, ZEnv, ZHub, ZLayer}

/** Define all dependencies here. Construct services using ZLayers. Merge contructed ZLayers with API layer. API Layer
  * is to be initialized and executed by zio.App
  */
object Layers {
  val logger: ULayer[Has[Logger[String]]] =
    Slf4jLogger.make { (context, message) =>
      context.get(LogAnnotation.CorrelationId).map(_.toString) match {
        case Some(corrId) => s"[corrId=$corrId] $message"
        case None         => message
      }
    }

  val basicEnv = ZLayer.requires[ZEnv] ++ logger

  val s3Client =
    (netty.default ++ Config.layer) >>> core.config.configured() >>> (s3.live ++ logger) >>> S3Client.layer

  val modelHub = ZHub.unbounded[Model].toLayer

  val db: ZLayer[Blocking, Throwable, Has[FlywayClient] with Has[PluginRepository] with Has[ModelRepository] with Has[
    ReportRepository
  ]] = {
    val dbLayer         = DataSourceLayer.fromPrefix(Config.databaseConfPrefix)
    val dbCtxLayer      = DatabaseContext.layer
    val flywayLayer     = (dbLayer ++ ZLayer.identity[Blocking] >>> FlywayClient.layer).tap(_.get.migrate())
    val deps            = dbLayer ++ dbCtxLayer
    val pluginRepoLayer = deps >>> PluginRepositoryImpl.layer
    val modelRepoLayer  = deps ++ modelHub >>> ModelRepositoryImpl.layer
    val reportRepoLayer = deps >>> ReportRepositoryImpl.layer
    flywayLayer ++ pluginRepoLayer ++ modelRepoLayer ++ reportRepoLayer
  }

  val sttp: Layer[Throwable, Has[SttpClient.Service]] = AsyncHttpClientZioBackend.layer()

  val pluginEndpoint =
    (ZLayer.requires[ZEnv] ++ db ++ sttp ++ Config.layer) >>> PluginEndpoint.layer
  val modelEndpoint  = db ++ s3Client >>> ModelEndpoint.layer
  val proxyEndpoint  = (sttp ++ db) >>> PluginProxyEndpoint.layer
  val reportEndpoint = db >>> ReportEndpoint.layer
  val api            = pluginEndpoint ++ modelEndpoint ++ proxyEndpoint ++ reportEndpoint

  val modelSub = (db ++ modelHub) >>> ModelSubscriptionManager.layer

  val inferenceSub =
    ((logger >>> S3ObjectIndex.layer) ++ s3Client ++ db ++ logger ++ ZLayer
      .requires[Random]) >>> InferenceSubscriptionService.layer

  val pushGateway = Config.layer ++ logger >>> MetricSender.layer

  val all =
    Config.layer ++ logger ++ db ++ api ++ modelSub ++ inferenceSub ++ pushGateway
}
