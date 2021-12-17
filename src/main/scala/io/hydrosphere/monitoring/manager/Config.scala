package io.hydrosphere.monitoring.manager

import io.github.vigoo.zioaws.core.config.CommonAwsConfig
import io.github.vigoo.zioaws.core.config.descriptors.commonAwsConfig
import zio.config._
import ConfigDescriptor._
import io.hydrosphere.monitoring.manager.domain.metrics.sender.PushGateway.{Password, Username}
import io.hydrosphere.monitoring.manager.domain.metrics.sender.PushGatewayConfig
import io.hydrosphere.monitoring.manager.util.URI
import zio.{system, Chunk, Has, ZIO, ZLayer}
import zio.config.magnolia.descriptor
import zio.config.typesafe.TypesafeConfigSource

import java.net.URL

case class EndpointConfig(
    httpHost: java.net.URI,
    httpPort: Int,
    httpMaxRequestSize: Int = 8192,
    grpcPort: Int
) {
  lazy val httpUri = {
    val parsedUri = sttp.model.Uri(httpHost)
    URI(parsedUri.port(httpPort))
  }
}

case class MetricsConfig(
    impl: MetricsConfig.MetricImpl
)

object MetricsConfig {

  sealed trait MetricImpl

  final case class PushGatewayConfig(
      url: URL,
      creds: Option[PushGatewayCreds]
  )                                           extends MetricImpl
  final case class PrometheusConfig(url: URL) extends MetricImpl

  final case class PushGatewayCreds(username: Username, password: Password)

}

object Config {

  /** Defines where to read configs. Reads from application.conf file and falls back into env variables
    */
  val sources: ZIO[system.System, ReadError[String], ConfigSource] = TypesafeConfigSource.fromDefaultLoader

  val endpointDesc: ConfigDescriptor[EndpointConfig] =
    nested("endpoint")(descriptor[EndpointConfig])

  val pushgatewayDesc: ConfigDescriptor[Option[MetricsConfig]] = nested("metrics")(
    descriptor[Option[MetricsConfig]].describe("Configures Prometheus Pushgateway access. Optional config.")
  )

  /** zio-aws has descriptor which looks for fileds without prefix. For this app, root prefix wasn't suitable, so I put
    * it inside `aws` prefix.
    */
  val awsDesc: ConfigDescriptor[CommonAwsConfig] = nested("aws")(commonAwsConfig)

  /** zio-qiull parses config automatically. Here I provide config prefix, where it can find required fields.
    */
  final val databaseConfPrefix = "db"

  final val configs = Chunk(awsDesc, pushgatewayDesc, endpointDesc)

  val layer = {
    val configs = for {
      src <- sources
      endpoint = endpointDesc.from(src)
      aws      = awsDesc.from(src)
      pg       = pushgatewayDesc.from(src)
      endpointVal <- ZIO.fromEither(read(endpoint))
      awsVal      <- ZIO.fromEither(read(aws))
      pgVal       <- ZIO.fromEither(read(pg))
    } yield Has.allOf(endpointVal, awsVal, pgVal)
    configs.toLayerMany
  }
}
