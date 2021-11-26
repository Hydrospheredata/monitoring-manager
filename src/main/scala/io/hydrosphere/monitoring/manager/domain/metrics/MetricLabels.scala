package io.hydrosphere.monitoring.manager.domain.metrics

final case class MetricLabels(labels: Map[String, String]) {
  lazy val toArray: Array[String] =
    labels.map { case (k, v) => s"$k=$v" }.toArray

  def ++(other: Map[String, String]): MetricLabels = MetricLabels(labels ++ other)
}

object MetricLabels {
  val empty: MetricLabels = MetricLabels(Map.empty)
}
