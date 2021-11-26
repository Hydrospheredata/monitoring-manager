package io.hydrosphere.monitoring.manager.domain.data

import io.github.vigoo.zioaws.s3
import io.github.vigoo.zioaws.s3.S3
import io.github.vigoo.zioaws.s3.model.{HeadObjectRequest, HeadObjectResponse}
import io.hydrosphere.monitoring.manager.util.URI
import sttp.model.Uri
import zio._
import zio.logging.{LogAnnotation, Logger}
import zio.macros.accessible
import zio.stream.ZStream

@accessible
trait S3Client {
  def getPrefixData(prefix: URI): ZStream[Any, Throwable, S3Obj]
  def headObject(path: URI): ZIO[Any, Throwable, HeadObjectResponse.ReadOnly]
  def objectExists(path: URI): ZIO[Any, Nothing, Boolean]
}

object S3Client {
  case class Impl(s3Client: s3.S3.Service, log: Logger[String]) extends S3Client {
    def headObject(path: URI): ZIO[Any, Throwable, HeadObjectResponse.ReadOnly] =
      for {
        bucket <- path.bucketName
        req = HeadObjectRequest(
          bucket = bucket,
          key = path.objectPath
        )
        res <- s3Client
          .headObject(req)
          .mapError(_.toThrowable)
      } yield res

    def objectExists(path: URI): ZIO[Any, Nothing, Boolean] =
      headObject(path)
        .tapError(ex =>
          log.locally(LogAnnotation.Throwable(Some(ex))) {
            log.debug(s"Couldn't access $path object")
          }
        )
        .either
        .map(_.isRight)

    def getPrefixData(prefix: URI): ZStream[Any, Throwable, S3Obj] = for {
      bucket <- ZStream.fromEffect(prefix.bucketName)
      request = s3.model.ListObjectsV2Request(
        bucket = bucket,
        prefix = Some(prefix.objectPath)
      )
      data <- s3Client
        .listObjectsV2(request)
        .mapBoth(_.toThrowable, x => S3Obj.fromObj(bucket, x))
        .collect { case Some(obj) => obj }
    } yield data
  }

  val layer = (Impl.apply _).toLayer[S3Client]
}
