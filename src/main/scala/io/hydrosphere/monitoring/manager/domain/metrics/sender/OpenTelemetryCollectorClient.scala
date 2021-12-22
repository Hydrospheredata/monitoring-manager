package io.hydrosphere.monitoring.manager.domain.metrics.sender
import io.grpc.Status
import io.hydrosphere.monitoring.manager.domain.metrics.sender.MetricSender.JobName
import io.hydrosphere.monitoring.manager.domain.metrics.sender.OpenTelemetryCollectorClient.{
  ExportError,
  MetricConversionError
}
import io.hydrosphere.monitoring.manager.util.URI
import io.opentelemetry.proto.collector.metrics.v1.metrics_service.ExportMetricsServiceRequest
import io.opentelemetry.proto.collector.metrics.v1.metrics_service.ZioMetricsService.MetricsServiceClient
import io.prometheus.client.CollectorRegistry
import zio.ZIO

final case class OpenTelemetryCollectorClient(collectorUrl: URI, metricsServiceClient: MetricsServiceClient.Service)
    extends MetricSender {
  override def push(registry: CollectorRegistry, jobName: JobName): ZIO[Any, SendError, Unit] =
    for {
      request  <- ZIO(ExportMetricsServiceRequest()).mapError(MetricConversionError.apply)
      response <- metricsServiceClient.`export`(request).mapError(ExportError.apply)
    } yield ()
}

object OpenTelemetryCollectorClient {
  final case class MetricConversionError(err: Throwable)
      extends SendError("Can't convert metrics to OTLP format", Some(err))
  final case class ExportError(status: Status)
      extends SendError("GRPC error while sending metrics", Some(status.asException()))
}
