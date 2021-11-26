package io.hydrosphere.monitoring.manager.domain.metrics

import io.hydrosphere.monitoring.manager.domain.metrics.PushGateway.{JobName, Password, Username}
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.{
  BasicAuthHttpConnectionFactory,
  DefaultHttpConnectionFactory,
  HttpConnectionFactory,
  PushGateway => PPG
}
import zio.logging.{log, Logger, Logging}
import zio.macros.accessible
import zio.metrics.prometheus.Registry
import zio._

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

  val layer: ZLayer[Logging with Has[Option[PushGatewayConfig]], Throwable, Has[PushGateway]] =
    ZLayer
      .service[Option[PushGatewayConfig]]
      .flatMap { x =>
        x.get match {
          case Some(value) =>
            (ZIO(value).toLayer ++ ZLayer.identity[Logging] >>> PushGatewayImpl.layer)
              .tap(_ => log.info("Using PushGateway"))
          case None =>
            (ZLayer.identity[Logging] >>> PushGatewayNoopImpl.layer).tap(_ => log.info("No PushGateway integration"))
        }
      }
}
final case class PushError(jobName: JobName, underlying: Throwable)
    extends Error(s"Can't push $jobName job to the PushGateway", underlying)

/** Noop is used when there is no PG configuration.
  */
case class PushGatewayNoopImpl(log: Logger[String]) extends PushGateway {
  override def push(registry: CollectorRegistry, jobName: JobName): ZIO[Any, PushError, Unit] =
    log.debug("no-op push").unit
}

object PushGatewayNoopImpl {
  val layer = (PushGatewayNoopImpl.apply _).toLayer[PushGateway]
}

final case class PushGatewayImpl(exporter: PPG, logger: Logger[String]) extends PushGateway {
  def push(registry: CollectorRegistry, jobName: JobName): ZIO[Any, PushError, Unit] =
    logger.debug(s"Pushing $jobName") *>
      Task {
        exporter.push(registry, jobName)
      }.mapError(PushError(jobName, _))
}

object PushGatewayImpl {
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

  val layer = (for {
    config <- ZIO.service[PushGatewayConfig]
    log    <- ZIO.service[Logger[String]]
    hcf    <- makeHttpConnFactory(config.creds)
    pg     <- makeUnsafePG(config.url, hcf)
  } yield PushGatewayImpl(pg, log): PushGateway).toLayer
}

final case class PushGatewayConfig(
    url: URL,
    creds: Option[PushGatewayCreds]
)

final case class PushGatewayCreds(username: Username, password: Password)
