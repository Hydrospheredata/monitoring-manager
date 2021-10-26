package io.hydrosphere.monitoring.manager.domain.data

import io.github.vigoo.zioaws.s3.model.S3Object
import io.hydrosphere.monitoring.manager.util.UriUtil

import java.time.Instant

case class S3Obj(bucket: String, key: String, lastModified: Instant) {
  def fullPath = UriUtil.s3Path(bucket, key)
}

object S3Obj {
  def fromObj(bucket: String, s3: S3Object.ReadOnly) =
    for {
      key          <- s3.keyValue
      lastModified <- s3.lastModifiedValue
    } yield S3Obj(bucket, key, lastModified)
}
