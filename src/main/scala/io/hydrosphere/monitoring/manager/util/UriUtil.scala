package io.hydrosphere.monitoring.manager.util

import io.circe.{Decoder, Encoder}
import io.github.vigoo.zioaws.s3.model.S3Object
import sttp.model.{QueryParams, Uri}
import sttp.model.Uri.QuerySegment
import sttp.model.Uri.QuerySegment.{KeyValue, Value}

import scala.collection.immutable.Seq

object UriUtil {
  def s3Path(bucket: String, s3Key: String) =
    Uri.apply(scheme = "s3", host = bucket, path = s3Key.split("/"))

  def combine(baseUri: Uri, segment: Uri): Uri =
    baseUri.addPathSegments(segment.pathSegments.segments)

  def combine(baseUri: Uri, segments: Seq[String]) =
    baseUri.addPath(segments)

  def queryPassthrough(mqp: QueryParams): Iterable[QuerySegment] =
    mqp.toMultiSeq.flatMap { case (k, vs) =>
      vs match {
        case Seq() => List(Value(k))
        case s     => s.map(v => KeyValue(k, v))
      }
    }

  object implicits {
    implicit val uriEncoder = Encoder.encodeString.contramap[Uri](_.toString())
    implicit val uriDecoder = Decoder.decodeString.emap[Uri](Uri.parse)
  }
}
