package io.hydrosphere.monitoring.manager.domain.metrics.sender

import io.hydrosphere.monitoring.manager.domain.metrics.sender.DirectPusher.{
  writeRequestFormat,
  SnappyCompressionError,
  WriteRequestError
}
import io.hydrosphere.monitoring.manager.domain.metrics.sender.SendError
import io.hydrosphere.monitoring.manager.domain.metrics.sender.PushGateway.JobName
import io.hydrosphere.monitoring.manager.util.{Snappy, URI}
import io.hydrosphere.monitoring.manager.util.URI.Context
import io.prometheus.client.CollectorRegistry
import prometheus.remote.WriteRequest
import sttp.client3.asynchttpclient.zio.SttpClient
import sttp.client3.basicRequest
import zio.logging.Logger
import scala.jdk.CollectionConverters._

case class DirectPusher(baseUrl: URI, sttpClient: SttpClient.Service, log: Logger[String]) extends MetricSender {
  final val WRITE_URL = uri"/api/v1/write"

  final val FULL_URL = baseUrl.u.addPathSegments(WRITE_URL.u.pathSegments.segments)

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
      snappyBytes <- Snappy.compress(remoteWrite.toByteArray).mapError(SnappyCompressionError.apply)
      req = basicRequest
        .post(FULL_URL)
        .body(snappyBytes)
      resp <- sttpClient.send(req).mapError(WriteRequestError.apply)
      _    <- log.debug(s"Got resp from Prometheus: $resp")
    } yield ()
  }
}

object DirectPusher {
  final case class SnappyCompressionError(underlying: Throwable)
      extends SendError(s"Can't apply snappy compression to WriteRequest", underlying)
  final case class WriteRequestError(underlying: Throwable) extends SendError(s"Can't send WriteRequest", underlying)

  def writeRequestFormat(registry: CollectorRegistry): WriteRequest =
//    registry.metricFamilySamples().asScala.map { s =>
//      s.samples.asScala.map{ ss =>
//
//      }
//    }
    WriteRequest.defaultInstance
}
