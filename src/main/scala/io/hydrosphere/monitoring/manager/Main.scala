package io.hydrosphere.monitoring.manager

import io.getquill.context.ZioJdbc._
import io.github.vigoo.zioaws._
import io.hydrosphere.monitoring.manager.api.grpc.GRPCServer
import io.hydrosphere.monitoring.manager.api.http.{HTTPServer, PluginEndpoint}
import io.hydrosphere.monitoring.manager.db.{DatabaseContext, FlywayClient}
import io.hydrosphere.monitoring.manager.domain.clouddriver.CloudDriverImpl
import io.hydrosphere.monitoring.manager.domain.plugin.PluginRepositoryImpl
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._
import zio.logging._
import zio.logging.slf4j._

/** Define all dependencies here. Construct services using ZLayers. Merge contructed ZLayers with
  * API layer. API Layer is to be initialized and executed by zio.App
  */
object Layers {
  val logger = {
    val logFormat = "[corr-id=%s] %s"
    Slf4jLogger.make { (context, message) =>
      val correlationId =
        context.get(LogAnnotation.CorrelationId).map(_.toString).getOrElse("undefined")
      logFormat.format(correlationId, message)
    }
  }

  val aws =
    (netty.default ++ Config.layer) >>> core.config.configured() >>> s3.live

  val db = {
    val dbLayer         = DataSourceLayer.fromPrefix(Config.databaseConfPrefix)
    val dbCtxLayer      = DatabaseContext.layer
    val flywayLayer     = (dbLayer >>> FlywayClient.layer).tap(_.get.migrate())
    val pluginRepoLayer = (dbLayer ++ dbCtxLayer) >>> PluginRepositoryImpl.layer
    flywayLayer ++ pluginRepoLayer
  }

  val cloudDriver = CloudDriverImpl.layer

  val sttp = AsyncHttpClientZioBackend.layer()

  val api = (ZLayer.requires[ZEnv] ++ db ++ cloudDriver ++ sttp) >>> PluginEndpoint.layer

  val all =
    Config.layer ++ logger
}

object Main extends zio.App {
  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val servers = for {
      _ <- GRPCServer.start.useForever.forkAs("grpc-server")
      _ <- HTTPServer.start
        .forkAs("http-server")
    } yield ()
    servers
      .flatMap(_ => ZIO.never)
      .provideCustomLayer(Config.layer ++ Layers.logger ++ Layers.db ++ Layers.api)
      .exitCode
  }
}
