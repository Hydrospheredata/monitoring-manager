package io.hydrosphere.monitoring.manager.domain.metrics

import io.hydrosphere.monitoring.manager.domain.metrics.PushGateway.JobName
import io.prometheus.client.CollectorRegistry
import zio.{Has, ZIO, ZLayer}
import zio.metrics.prometheus.Registry

object MetricsService {
  val emptyCollector: ZLayer[Any, Throwable, Registry] =
    ZIO.effect(Option(new CollectorRegistry())).toLayer >>> Registry.explicit

  def sendMeasurable[T](jobName: JobName, m: T)(implicit
      measurable: Measurable[T]
  ): ZIO[Has[PushGateway], Throwable, Unit] =
    (for {
      _ <- measurable.measure(m, MetricLabels.empty)
      _ <- PushGateway.pushEnv(jobName)
    } yield ()).provideSomeLayer[Has[PushGateway]](emptyCollector)
}
