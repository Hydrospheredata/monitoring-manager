package io.hydrosphere.monitoring.manager

import io.hydrosphere.monitoring.manager.api.grpc.GRPCServer
import io.hydrosphere.monitoring.manager.api.http.HTTPServer
import io.hydrosphere.monitoring.manager.domain.data.InferenceSubscriptionService
import zio._
import zio.logging.log

object Main extends zio.App {
  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val servers = for {
      _   <- GRPCServer.start.useForever.forkAs("grpc-server")
      _   <- HTTPServer.start.forkAs("http-server")
      sub <- ZIO.service[InferenceSubscriptionService]
      _ <- sub.startMonitoring().forkAs("s3-monitor") <* log.info(
        "S3 object monitoring started"
      )
    } yield ()
    (servers *> ZIO.never)
      .provideCustomLayer(Layers.all)
      .exitCode
  }
}
