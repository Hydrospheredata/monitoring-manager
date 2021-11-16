package io.hydrosphere.monitoring.manager.domain.data

import io.circe.generic.JsonCodec
import io.github.vigoo.zioaws.s3.model.S3Object
import io.hydrosphere.monitoring.manager.util.{URI, UriUtil}
import io.circe.generic.semiauto._

import java.time.Instant

@JsonCodec
case class S3Obj private (bucket: String, key: String, lastModified: Instant, fullPath: URI)

object S3Obj {
  def apply(bucket: String, key: String, lastModified: Instant): S3Obj = {
    val fullPath = URI(UriUtil.s3Path(bucket, key))
    S3Obj.apply(bucket, key, lastModified, fullPath)
  }

  def fromObj(bucket: String, s3: S3Object.ReadOnly) =
    for {
      key          <- s3.keyValue
      lastModified <- s3.lastModifiedValue
    } yield S3Obj(bucket, key, lastModified)
}
