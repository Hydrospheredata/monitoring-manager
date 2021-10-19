package io.hydrosphere.monitoring.manager.domain.data

import io.github.vigoo.zioaws.s3
import io.github.vigoo.zioaws.s3.model.S3Object
import io.github.vigoo.zioaws.s3.S3
import sttp.model.Uri
import zio._
import zio.stream.ZStream

trait S3Client {
  def getPrefixData(prefix: Uri): ZStream[Any, Throwable, S3Object.ReadOnly]
}

object S3Client {
  case class Impl(s3Client: s3.S3.Service) extends S3Client {
    def getPrefixData(prefix: Uri) = for {
      bucket <- ZStream
        .fromEffect(ZIO.fromOption(prefix.host))
        .orElseFail(new IllegalArgumentException(s"Can't extract bucket name from $prefix"))
      strPrefix = prefix.pathSegments.toString
      request = s3.model.ListObjectsV2Request(
        bucket = bucket,
        prefix = Some(strPrefix)
      )
      data <- s3Client.listObjectsV2(request).mapError(_.toThrowable)
    } yield data
  }

  val layer: URLayer[Has[S3.Service], Has[S3Client]] = (Impl.apply _).toLayer
}
