package io.hydrosphere.monitoring.manager

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.github.vigoo.zioaws._
import io.github.vigoo.zioaws.core.config
import io.github.vigoo.zioaws.core.config.AwsConfig
import io.hydrosphere.monitoring.manager.api.http.{ModelEndpoint, PluginEndpoint}
import io.hydrosphere.monitoring.manager.db.{DatabaseContext, FlywayClient}
import io.hydrosphere.monitoring.manager.domain.data.{
  InferenceSubscriptionService,
  S3Client,
  S3ObjectIndex
}
import io.hydrosphere.monitoring.manager.domain.model.{
  Model,
  ModelRepository,
  ModelRepositoryImpl,
  ModelSubscriptionManager
}
import io.hydrosphere.monitoring.manager.domain.plugin.{PluginRepository, PluginRepositoryImpl}
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.{system, Has, ULayer, ZEnv, ZHub, ZLayer}
import zio.logging.slf4j.Slf4jLogger
import zio.logging.{LogAnnotation, Logger, Logging}

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

  val sttp = AsyncHttpClientZioBackend.layer()

  val pluginEndpoint = (ZLayer.requires[ZEnv] ++ db ++ sttp) >>> PluginEndpoint.layer
  val modelEndpoint  = db >>> ModelEndpoint.layer
  val api            = pluginEndpoint ++ modelEndpoint

  val modelSub = (db ++ modelHub) >>> ModelSubscriptionManager.layer

  val inferenceSub = S3ObjectIndex.layer ++ s3Client >>> InferenceSubscriptionService.layer

  type AppEnv = Has[EndpointConfig]
    with Has[config.CommonAwsConfig]
    with Has[Logger[String]]
    with Has[FlywayClient]
    with Has[PluginRepository]
    with Has[ModelRepository]
    with Has[PluginEndpoint]
    with Has[ModelEndpoint]
    with Has[ModelSubscriptionManager]
    with Has[InferenceSubscriptionService]

  val all: ZLayer[ZEnv, Throwable, AppEnv] =
    Config.layer ++ logger ++ db ++ api ++ modelSub ++ inferenceSub
}
