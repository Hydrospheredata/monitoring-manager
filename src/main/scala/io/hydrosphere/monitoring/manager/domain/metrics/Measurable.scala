package io.hydrosphere.monitoring.manager.domain.metrics

import zio._
import zio.metrics.prometheus._

trait Measurable[T] {
  def measure(obj: T, parentLabels: MetricLabels): ZIO[Registry, Throwable, Unit]
}
