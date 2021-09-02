package io.hydrosphere.monitoring.manager

import io.github.vigoo.zioaws.core.config.CommonAwsConfig
import io.github.vigoo.zioaws.core.config.descriptors.commonAwsConfig
import zio.config._
import ConfigDescriptor._
import zio.config.magnolia.descriptor
import zio.config.typesafe.TypesafeConfigSource
import zio.{system, Has, ZIO, ZLayer}

case class EndpointConfig(
    httpPort: Int,
    grpcPort: Int
)

object Config {

  /** Defines where to read configs. Reads from application.conf file and falls back into env
    * variables
    */
  val sources: ZIO[system.System, ReadError[String], ConfigSource] = for {
    config <- TypesafeConfigSource.fromDefaultLoader
    envs <- ConfigSource.fromSystemEnv(
      keyDelimiter = Some('_'),
      valueDelimiter = Some(',')
    )
  } yield config.orElse(envs)

  val endpointDesc: ConfigDescriptor[EndpointConfig] =
    nested("endpoint")(descriptor[EndpointConfig])

  /** zio-aws has descriptor which looks for fileds without prefix. For this app, root prefix wasn't
    * suitable, so I put it inside `aws` prefix.
    */
  val awsDesc: ConfigDescriptor[CommonAwsConfig] = nested("aws")(commonAwsConfig)

  /** zio-qiull parses config automatically. Here I provide config prefix, where it can find
    * required fields.
    */
  final val databaseConfPrefix = "db"

  val layer
      : ZLayer[system.System, ReadError[String], Has[EndpointConfig] with Has[CommonAwsConfig]] = {
    val configs = for {
      src <- sources
      endpoint = endpointDesc.from(src)
      aws      = awsDesc.from(src)
      endpointVal <- ZIO.fromEither(read(endpoint))
      awsVal      <- ZIO.fromEither(read(aws))
    } yield Has.allOf(endpointVal, awsVal)
    configs.toLayerMany
  }
}
