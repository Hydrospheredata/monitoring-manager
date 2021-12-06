package io.hydrosphere.monitoring.manager

import io.github.vigoo.zioaws.core.config.CommonAwsConfig
import io.github.vigoo.zioaws.core.config.descriptors.commonAwsConfig
import zio.config._
import ConfigDescriptor._
import io.hydrosphere.monitoring.manager.domain.metrics.PushGatewayConfig
import io.hydrosphere.monitoring.manager.util.URI
import zio.{system, Has, ZIO, ZLayer}
import zio.config.magnolia.descriptor
import zio.config.typesafe.TypesafeConfigSource

case class EndpointConfig(
    httpHost: java.net.URI,
    httpPort: Int,
    httpMaxRequestSize: Int = 2048,
    grpcPort: Int
) {
  lazy val httpUri = {
    val parsedUri = sttp.model.Uri(httpHost)
    URI(parsedUri.port(httpPort))
  }
}

object Config {

  /** Defines where to read configs. Reads from application.conf file and falls back into env variables
    */
  val sources: ZIO[system.System, ReadError[String], ConfigSource] = TypesafeConfigSource.fromDefaultLoader

  val endpointDesc: ConfigDescriptor[EndpointConfig] =
    nested("endpoint")(descriptor[EndpointConfig])

  val pushgatewayDesc: ConfigDescriptor[Option[PushGatewayConfig]] = nested("pushgateway")(
    descriptor[Option[PushGatewayConfig]].describe("Configures Prometheus Pushgateway access. Optional config.")
  )

  /** zio-aws has descriptor which looks for fileds without prefix. For this app, root prefix wasn't suitable, so I put
    * it inside `aws` prefix.
    */
  val awsDesc: ConfigDescriptor[CommonAwsConfig] = nested("aws")(commonAwsConfig)

  /** zio-qiull parses config automatically. Here I provide config prefix, where it can find required fields.
    */
  final val databaseConfPrefix = "db"

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
