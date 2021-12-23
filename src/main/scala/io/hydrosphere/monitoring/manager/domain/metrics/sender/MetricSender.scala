package io.hydrosphere.monitoring.manager.domain.metrics.sender

import io.hydrosphere.monitoring.manager.MetricsConfig
import io.hydrosphere.monitoring.manager.domain.metrics.Metric
import zio.logging.{log, Logger, Logging}
import zio.macros.accessible
import zio.{Has, ZIO, ZLayer}

import scala.util.control.NoStackTrace

@accessible
trait MetricSender {
  def push(registry: Seq[Metric]): ZIO[Any, SendError, Unit]
}

abstract class SendError(msg: String, cause: Option[Throwable])
    extends Error(msg, cause.getOrElse(new Exception with NoStackTrace))

object MetricSender {
  val layer: ZLayer[Has[Logger[String]] with Has[Option[MetricsConfig]], Throwable, Has[MetricSender]] =
    ZLayer.requires[Has[Option[MetricsConfig]]].flatMap { x =>
      x.get match {
        case Some(value) =>
          (ZLayer.succeed(value) ++ ZLayer.requires[Logging] >>> OpenTelemetryCollectorClient.layer)
            .tap(_ => log.info("Using OpenTelemetry collector for metrics"))
        case None => SenderNoopImpl.layer.tap(_ => log.info("Using noop collector for metrics"))
      }
    }
}
