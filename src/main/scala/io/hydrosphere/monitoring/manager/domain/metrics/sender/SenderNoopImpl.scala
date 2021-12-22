package io.hydrosphere.monitoring.manager.domain.metrics.sender

import io.hydrosphere.monitoring.manager.domain.metrics.sender.MetricSender.JobName
import io.prometheus.client.CollectorRegistry
import zio.ZIO
import zio.logging.Logger

/** Noop is used when there is no PG configuration.
  */
case class SenderNoopImpl(log: Logger[String]) extends MetricSender {
  override def push(registry: CollectorRegistry, jobName: JobName): ZIO[Any, Nothing, Unit] =
    log.debug("no-op push").unit
}

object SenderNoopImpl {
  val layer = (SenderNoopImpl.apply _).toLayer[MetricSender]
}
