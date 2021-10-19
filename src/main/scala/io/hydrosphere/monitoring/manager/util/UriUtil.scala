package io.hydrosphere.monitoring.manager.util

import io.circe.{Decoder, Encoder}
import sttp.model.Uri

object UriUtil {
  def combine(baseUri: Uri, segment: Uri): Uri =
    baseUri.addPathSegments(segment.pathSegments.segments)

  object implicits {
    implicit val uriEncoder = Encoder.encodeString.contramap[Uri](_.toString())
    implicit val uriDecoder = Decoder.decodeString.emap[Uri](Uri.parse)
  }
}
