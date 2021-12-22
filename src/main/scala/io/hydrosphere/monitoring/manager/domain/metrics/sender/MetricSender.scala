package io.hydrosphere.monitoring.manager.domain.metrics.sender

import io.hydrosphere.monitoring.manager.domain.metrics.sender.MetricSender.JobName
import io.prometheus.client.CollectorRegistry
import zio.macros.accessible
import zio.ZIO
import zio.metrics.prometheus.Registry

import scala.util.control.NoStackTrace

@accessible
trait MetricSender {
  def push(registry: CollectorRegistry, jobName: JobName): ZIO[Any, SendError, Unit]

  def pushEnv(jobName: JobName): ZIO[Registry, SendError, Unit] =
    ZIO.accessM[Registry](_.get.getCurrent()) >>= (push(_, jobName))
}

abstract class SendError(msg: String, cause: Option[Throwable])
    extends Error(msg, cause.getOrElse(new Exception with NoStackTrace))

object MetricSender {
  type JobName = String
  val layer = SenderNoopImpl.layer
}
