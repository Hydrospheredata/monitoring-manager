package io.hydrosphere.monitoring.manager.domain.metrics

import io.hydrosphere.monitoring.manager.domain.metrics.PushGateway.{JobName, Password, Username}
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.{
  BasicAuthHttpConnectionFactory,
  DefaultHttpConnectionFactory,
  HttpConnectionFactory,
  PushGateway => PPG
}
import zio.logging.{log, Logging}
import zio.macros.accessible
import zio.metrics.prometheus.Registry
import zio.{Has, Task, ZIO, ZLayer}

import java.net.URL

@accessible
trait PushGateway {

  /** Pushes metrics from given registry to the PushGateway.
    */
  def push(registry: CollectorRegistry, jobName: JobName): ZIO[Any, PushError, Unit]

  def pushEnv(jobName: JobName): ZIO[Registry, PushError, Unit] =
    ZIO.accessM[Registry](_.get.getCurrent()) >>= (push(_, jobName))
}

object PushGateway {
  type JobName  = String
  type Username = String
  type Password = String

  val layer: ZLayer[Has[Option[PushGatewayConfig]] with Logging, Throwable, Has[PushGateway]] =
    ZLayer
      .service[Option[PushGatewayConfig]]
      .flatMap { x =>
        x.get match {
          case Some(value) =>
            (ZIO(value).toLayer ++ ZLayer.identity[Logging] >>> Impl.layer).tap(_ => log.info("Using PushGateway"))
          case None =>
            (ZLayer.identity[Logging] >>> NoopImpl.layer).tap(_ => log.info("No PushGateway integration"))
        }
      }
}
final case class PushError(jobName: JobName, underlying: Throwable)
    extends Error(s"Can't push $jobName job to the PushGateway", underlying)

/** Noop is used when there is no PG configuration.
  */
case object NoopImpl extends PushGateway {
  override def push(registry: CollectorRegistry, jobName: JobName): ZIO[Any, PushError, Unit] = ZIO.unit

  val layer: ZLayer[Any, Throwable, Has[PushGateway]] = ZIO(NoopImpl: PushGateway).toLayer
}

final case class Impl(exporter: PPG) extends PushGateway {
  def push(registry: CollectorRegistry, jobName: JobName): ZIO[Any, PushError, Unit] = Task {
    exporter.push(registry, jobName)
  }.mapError(PushError(jobName, _))
}

object Impl {
  def makeHttpConnFactory(creds: Option[PushGatewayCreds]): Task[HttpConnectionFactory] = ZIO.effect {
    val basicAuth = creds.map(c => new BasicAuthHttpConnectionFactory(c.username, c.password))
    val default   = new DefaultHttpConnectionFactory()
    basicAuth.getOrElse(default)
  }

  def makeUnsafePG(url: URL, httpConnectionFactory: HttpConnectionFactory): Task[PPG] = ZIO.effect {
    val ppg = new PPG(url)
    ppg.setConnectionFactory(httpConnectionFactory)
    ppg
  }

  val layer: ZLayer[Has[PushGatewayConfig], Throwable, Has[PushGateway]] = (for {
    config <- ZIO.service[PushGatewayConfig]
    hcf    <- makeHttpConnFactory(config.creds)
    pg     <- makeUnsafePG(config.url, hcf)
  } yield Impl(pg): PushGateway).toLayer
}

final case class PushGatewayConfig(
    url: URL,
    creds: Option[PushGatewayCreds]
)

final case class PushGatewayCreds(username: Username, password: Password)
