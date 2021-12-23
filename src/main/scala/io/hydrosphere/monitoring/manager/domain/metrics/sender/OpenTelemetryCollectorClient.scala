package io.hydrosphere.monitoring.manager.domain.metrics.sender
import io.grpc.Status
import io.grpc.netty.NettyChannelBuilder
import io.hydrosphere.monitoring.manager.MetricsConfig
import io.hydrosphere.monitoring.manager.domain.metrics.Metric
import io.opentelemetry.proto.collector.metrics.v1.metrics_service.ExportMetricsServiceRequest
import io.opentelemetry.proto.collector.metrics.v1.metrics_service.ZioMetricsService.MetricsServiceClient
import io.opentelemetry.proto.collector.metrics.v1.metrics_service.ZioMetricsService.MetricsServiceClient.Service
import io.opentelemetry.proto.common.v1.common.{AnyValue, InstrumentationLibrary, KeyValue}
import io.opentelemetry.proto.metrics.v1.metrics.{
  Gauge,
  InstrumentationLibraryMetrics,
  NumberDataPoint,
  ResourceMetrics,
  Metric => OMetric
}
import scalapb.zio_grpc.ZManagedChannel
import zio._
import zio.logging.{Logger, Logging}

final case class OpenTelemetryCollectorClient(metricsServiceClient: MetricsServiceClient.Service, log: Logger[String])
    extends MetricSender {
  override def push(registry: Seq[Metric]): ZIO[Any, SendError, Unit] = {
    val request = OpenTelemetryCollectorClient.convertToProto(registry)
    for {
      _        <- log.debug(s"Sending to OTLP collector: $request")
      response <- metricsServiceClient.`export`(request).mapError(OpenTelemetryCollectorClient.ExportError.apply)
      _        <- log.debug(s" OTLP collector response: $response")
    } yield ()
  }
}

object OpenTelemetryCollectorClient {
  final val INSTRUMENTATION_LIBRARY = InstrumentationLibrary.of("hydro-monitoring", "0.1.0")

  def convertToProto(registry: Seq[Metric]): ExportMetricsServiceRequest = {
    val requests = registry.map { s =>
      val labels = s.labels.map { case (k, v) => KeyValue(k, Some(AnyValue(AnyValue.Value.StringValue(v)))) }.toSeq
      ResourceMetrics(
        instrumentationLibraryMetrics = Seq(
          InstrumentationLibraryMetrics(
            instrumentationLibrary = Some(INSTRUMENTATION_LIBRARY),
            metrics = Seq(
              OMetric.of(
                name = s.name,
                description = s.description,
                unit = s.unit,
                data = OMetric.Data.Gauge.apply(
                  Gauge.of(
                    Seq(
                      NumberDataPoint(
                        attributes = labels,
                        timeUnixNano = (s.timestamp.getEpochSecond * 1000000000) + s.timestamp.getNano,
//                        timeUnixNano = s.timestamp.getEpochSecond,
                        value = NumberDataPoint.Value.AsDouble(s.value)
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    }
    ExportMetricsServiceRequest(requests)
  }

  final case class MetricConversionError(err: Throwable)
      extends SendError("Can't convert metrics to OTLP format", Some(err))

  final case class ExportError(status: Status)
      extends SendError("GRPC error while sending metrics", Some(status.asException()))

  val client = for {
    config <- ZLayer.requires[Has[MetricsConfig]]
    channel = {
      val builder = NettyChannelBuilder.forTarget(config.get.collectorUri.toString).usePlaintext()
      ZManagedChannel.apply(builder)
    }
    client <- MetricsServiceClient.live(channel)
  } yield client

  def make(x: Service, log: Logger[String]): MetricSender =
    OpenTelemetryCollectorClient.apply(x, log): MetricSender

  val layer =
    (client ++ ZLayer.requires[zio.logging.Logging]) >>> (make _).toLayer
}
