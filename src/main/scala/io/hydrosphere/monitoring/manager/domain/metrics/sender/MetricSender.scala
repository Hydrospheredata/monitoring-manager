package io.hydrosphere.monitoring.manager.domain.metrics.sender

import PushGateway.JobName
import io.prometheus.client.CollectorRegistry
import zio.macros.accessible
import zio.ZIO
import zio.metrics.prometheus.Registry

@accessible
trait MetricSender {
  def push(registry: CollectorRegistry, jobName: JobName): ZIO[Any, SendError, Unit]

  def pushEnv(jobName: JobName): ZIO[Registry, SendError, Unit] =
    ZIO.accessM[Registry](_.get.getCurrent()) >>= (push(_, jobName))
}

abstract class SendError(msg: String, cause: Throwable) extends Error(msg, cause)
