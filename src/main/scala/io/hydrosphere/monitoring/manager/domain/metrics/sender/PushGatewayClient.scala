package io.hydrosphere.monitoring.manager.domain.metrics.sender

import io.hydrosphere.monitoring.manager.domain.metrics.sender.MetricSender.JobName
import io.hydrosphere.monitoring.manager.domain.metrics.sender.PushGatewayImpl.{Password, Username}
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.{
  BasicAuthHttpConnectionFactory,
  DefaultHttpConnectionFactory,
  HttpConnectionFactory,
  PushGateway => PPG
}
import zio._
import zio.blocking.Blocking
import zio.logging._

import java.net.URL

final case class PushError(jobName: JobName, underlying: Throwable)
    extends SendError(s"Can't push $jobName job to the PushGateway", Some(underlying))

final case class PushGatewayImpl(exporter: PPG, logger: Logger[String], blocking: Blocking.Service)
    extends MetricSender {
  def push(registry: CollectorRegistry, jobName: JobName) =
    logger.debug(s"Pushing $jobName") *>
      blocking.blocking {
        Task {
          exporter.push(registry, jobName)
        }.mapError(PushError(jobName, _))
      }
}

object PushGatewayImpl {
  type Username = String
  type Password = String

//  val layer =
//    ZLayer
//      .service[Option[PushGatewayConfig]]
//      .flatMap { x =>
//        x.get match {
//          case Some(value) =>
//            (ZIO(value).toLayer ++ ZLayer.identity[Logging] ++ ZLayer.identity[Blocking] >>> PushGatewayImpl.layer)
//              .tap(_ => log.info("Using PushGateway"))
//          case None =>
//            (ZLayer.identity[Logging] >>> SenderNoopImpl.layer).tap(_ => log.info("No PushGateway integration"))
//        }
//      }

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
    block  <- ZIO.service[Blocking.Service]
    hcf    <- makeHttpConnFactory(config.creds)
    pg     <- makeUnsafePG(config.url, hcf)
  } yield PushGatewayImpl(pg, log, block): MetricSender).toLayer
}

final case class PushGatewayConfig(
    url: URL,
    creds: Option[PushGatewayCreds]
)

final case class PushGatewayCreds(username: Username, password: Password)
