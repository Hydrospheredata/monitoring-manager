package io.hydrosphere.monitoring.manager.domain.metrics

import java.time.Instant

final case class Metric(
    name: String,
    value: Double,
    description: String,
    unit: String,
    labels: Map[String, String],
    timestamp: Instant
)
