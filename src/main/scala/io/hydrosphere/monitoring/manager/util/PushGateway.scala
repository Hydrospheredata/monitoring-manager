package io.hydrosphere.monitoring.manager.util

import io.hydrosphere.monitoring.manager.util.PushGateway.{JobName, PushError}
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.{
  BasicAuthHttpConnectionFactory,
  DefaultHttpConnectionFactory,
  HttpConnectionFactory,
  PushGateway => PPG
}
import zio.{Has, Task, ZIO, ZLayer}

trait PushGateway {

  /** Pushes metrics from given registry to the PushGateway.
    */
  def push(registry: CollectorRegistry, jobName: JobName): ZIO[Any, PushError, Unit]
}

object PushGateway {
  type JobName  = String
  type Username = String
  type Password = String

  final case class PushError(jobName: JobName, underlying: Throwable)
      extends Error(s"Can't push $jobName job to the PushGateway", underlying)

  final case class Impl(exporter: PPG) extends PushGateway {
    def push(registry: CollectorRegistry, jobName: JobName): ZIO[Any, PushError, Unit] = Task {
      exporter.push(registry, jobName)
    }.mapError(PushError(jobName, _))
  }

  final case class PushGatewayConfig(
      url: URI,
      creds: Option[PushGatewayCreds]
  )

  final case class PushGatewayCreds(username: Username, password: Password)

  def makeHttpConnFactory(creds: Option[PushGatewayCreds]): Task[HttpConnectionFactory] = ZIO.effect {
    val basicAuth = creds.map(c => new BasicAuthHttpConnectionFactory(c.username, c.password))
    val default   = new DefaultHttpConnectionFactory()
    basicAuth.getOrElse(default)
  }

  def makeUnsafePG(url: URI, httpConnectionFactory: HttpConnectionFactory): Task[PPG] = ZIO.effect {
    val ppg = new PPG(url.u.toJavaUri.toURL)
    ppg.setConnectionFactory(httpConnectionFactory)
    ppg
  }

  val layer: ZLayer[Has[PushGatewayConfig], Throwable, Has[PushGateway]] = (for {
    config <- ZIO.service[PushGatewayConfig]
    hcf    <- makeHttpConnFactory(config.creds)
    pg     <- makeUnsafePG(config.url, hcf)
  } yield Impl(pg): PushGateway).toLayer
}
