package io.hydrosphere.monitoring.manager.domain.metrics.sender

import io.hydrosphere.monitoring.manager.domain.metrics.sender.MetricSender.JobName
import io.hydrosphere.monitoring.manager.domain.metrics.sender.RemoteWriteClient.{
  writeRequestFormat,
  ErrorResponse,
  SnappyCompressionError,
  WriteRequestError
}
import io.hydrosphere.monitoring.manager.util.URI.Context
import io.hydrosphere.monitoring.manager.util.{Snappy, URI}
import io.prometheus.client.CollectorRegistry
import prometheus.remote.WriteRequest
import prometheus.types.{Label, Sample, TimeSeries}
import sttp.client3.asynchttpclient.zio.SttpClient
import sttp.client3.basicRequest
import zio.ZIO
import zio.logging.Logger

import java.time.Instant
import scala.jdk.CollectionConverters._

/** Prototype for Prometheus remote-write API.
  *
  * @param url
  *   @param sttpClient
  * @param log
  */
case class RemoteWriteClient(url: URI, sttpClient: SttpClient.Service, log: Logger[String]) extends MetricSender {
  final val WRITE_URL = uri"/api/v1/write"

  /** Pushes metrics from given registry directly to the Prometheus instance.
    *
    * Uses
    * [[https://prometheus.io/docs/prometheus/latest/storage/#remote-storage-integrations Remote storage integration]].
    * Needs a Prometheus instance with `--enable-feature=remote-write-receiver` argument. Writes data to the
    * `/api/v1/write` URL.
    */
  override def push(registry: CollectorRegistry, jobName: JobName) = {
    val remoteWrite = writeRequestFormat(registry)
    for {
      _           <- log.info(s"Sending to Prometheus: ${remoteWrite.toProtoString}")
      snappyBytes <- Snappy.compress(remoteWrite.toByteArray).mapError(SnappyCompressionError.apply)
      req = basicRequest
        .post(url.u)
        .headers(
          Map(
            "Content-Encoding"                  -> "snappy",
            "Content-Type"                      -> "application/x-protobuf",
            "X-Prometheus-Remote-Write-Version" -> "0.1.0",
            "User-Agent"                        -> "hydromonitoring"
          )
        )
        .body(snappyBytes)
      resp <- sttpClient.send(req).mapError(WriteRequestError.apply)
      _    <- ZIO.fromEither(resp.body).mapError(ErrorResponse.apply)
      _    <- log.info(s"Got resp from Prometheus: $resp")
    } yield ()
  }
}

object RemoteWriteClient {
  final case class SnappyCompressionError(underlying: Throwable)
      extends SendError(s"Can't apply snappy compression to WriteRequest", Some(underlying))
  final case class ErrorResponse(msg: String) extends SendError(msg, None)
  final case class WriteRequestError(underlying: Throwable)
      extends SendError(s"Can't send WriteRequest", Some(underlying))

  def writeRequestFormat(registry: CollectorRegistry): WriteRequest = {
    val requests = registry.metricFamilySamples().asScala.flatMap { s =>
      s.samples.asScala.map { ss =>
        val labels = ss.labelNames.asScala
          .zip(ss.labelValues.asScala)
          .map { case (name, value) => Label.of(name, value) }
          .toSeq :+ Label.of("__name__", s.name)
        val samples = Seq(
          Sample.of(
            value = ss.value,
            timestamp = Instant.now().toEpochMilli
          )
        )

        val ts = TimeSeries.of(
          labels = labels,
          samples = samples,
          exemplars = Seq.empty
        )
        WriteRequest.of(
          timeseries = Seq(ts),
          metadata = Seq.empty
        )
      }
    }
    requests.fold(WriteRequest.defaultInstance) { case (a, b) =>
      a.copy(timeseries = a.timeseries ++ b.timeseries, metadata = a.metadata ++ b.metadata)
    }
  }
}
