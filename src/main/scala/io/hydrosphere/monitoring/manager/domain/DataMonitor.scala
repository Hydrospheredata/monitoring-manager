package io.hydrosphere.monitoring.manager.domain

import io.github.vigoo.zioaws.s3
import io.github.vigoo.zioaws.s3.model.{ListObjectsV2Request, S3Object}
import sttp.model.Uri
import zio._

import scala.concurrent.duration._
import scala.collection.mutable.{Set => MutableSet}

case class NewModelData(model: Model, key: String)

final case class Model(name: String, version: Long)

object Model {

  /** @param url
    *   needs to be without prefix
    * @return
    */
  def fromUrl(uri: Uri) =
    for {
      name <- ZIO
        .fromOption(uri.pathSegments.segments.headOption)
        .mapBoth(_ => new IllegalArgumentException(s"Can't extract model name from $uri"), _.v)
      versionStr <- ZIO
        .fromOption(uri.pathSegments.segments.tail.headOption)
        .mapBoth(_ => new IllegalArgumentException(s"Can't extract model version from $uri"), _.v)
      version <- ZIO
        .fromOption(versionStr.toLongOption)
        .orElseFail {
          new IllegalArgumentException(
            s"Can't convert model version ($versionStr) to Long from $uri"
          )
        }
    } yield Model(name, version)
}

final case class ObjectIndex(
    private val stupidIndex: MutableSet[String] = MutableSet.empty[String]
) {
  def isExisting(obj: S3Object.ReadOnly) = for {
    key <- obj.key
    res <- ZIO.effect(stupidIndex.add(key))
    _   <- logging.log.debug(s"Got: $key, added? = $res")
  } yield res
}

final case class DataMonitor(
    bucket: String,
    prefix: Option[String],
    schedule: FiniteDuration = 1.minute,
    index: ObjectIndex = ObjectIndex()
) {
  def start = {
    val request = ListObjectsV2Request(
      bucket = bucket,
      prefix = prefix
    )
    s3.listObjectsV2(request)
      .filterM(index.isExisting)
      .mapM(DataMonitor.parseObject(prefix, _))
  }
}

object DataMonitor {
  final val TrainingDataPrefix  = "training-data"
  final val InferenceDataPrefix = "inference-data"

  def parseObject(prefix: Option[String], obj: S3Object.ReadOnly) =
    for {
      key    <- obj.key
      uriKey <- ZIO.fromEither(Uri.parse(key))
      firstSegment <- ZIO
        .fromOption(uriKey.pathSegments.segments.headOption)
        .orElseFail(new IllegalArgumentException(uriKey.toString()))
      keySegments = prefix
        .filter(p => p == firstSegment.v)
        .fold(uriKey.pathSegments.segments)(_ => uriKey.pathSegments.segments.tail)
      objectUri = uriKey.withPathSegments(keySegments)
      model <- Model.fromUrl(objectUri)
    } yield NewModelData(model, key)
}
