package io.hydrosphere.monitoring.manager.domain.metrics

final case class MetricLabels(labels: Map[String, String]) {
  lazy val toArray: Array[String] =
    labels.toSeq.flatMap(x => Seq(x._1, x._2)).toArray

  lazy val keys: Array[String] = labels.keys.toArray

  lazy val values: Array[String] = labels.values.toArray

  def ++(other: Map[String, String]): MetricLabels = MetricLabels(labels ++ other)
}

object MetricLabels {
  val empty: MetricLabels = MetricLabels(Map.empty)
}
