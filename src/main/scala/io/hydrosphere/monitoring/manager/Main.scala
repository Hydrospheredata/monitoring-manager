package io.hydrosphere.monitoring.manager

import io.hydrosphere.monitoring.manager.api.grpc.GRPCServer
import io.hydrosphere.monitoring.manager.api.http.HTTPServer
import zio._

object Main extends zio.App {
  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val servers = for {
      _ <- GRPCServer.start.useForever.forkAs("grpc-server")
      _ <- HTTPServer.start.forkAs("http-server")
    } yield ()
    (servers *> ZIO.never)
      .provideCustomLayer(Layers.all)
      .exitCode
  }
}
