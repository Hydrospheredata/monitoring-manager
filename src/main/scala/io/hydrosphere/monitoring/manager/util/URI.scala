package io.hydrosphere.monitoring.manager.util

import io.circe._
import io.getquill.MappedEncoding
import sttp.model.Uri
import sttp.tapir.Schema

case class URI(u: Uri) extends AnyVal

object URI {
  def parse(str: String)              = Uri.parse(str).map(URI.apply)
  def fromJava(javaURI: java.net.URI) = URI(Uri(javaURI))

  implicit val jsonuriEncoder: Encoder[URI] =
    Encoder.encodeString.contramap[Uri](_.toString()).contramap[URI](_.u)
  implicit val jsonuriDecoder: Decoder[URI] =
    Decoder.decodeString.emap[Uri](Uri.parse).map(URI.apply)

  implicit val uriDecoder: MappedEncoding[URI, String] =
    MappedEncoding[URI, String](x => x.u.toString())
  implicit val uriEncoder: MappedEncoding[String, URI] = MappedEncoding[String, URI](x =>
    Uri.parse(x).map(URI.apply).fold(err => throw new IllegalArgumentException(err), identity)
  )

  implicit val schema: Schema[URI] =
    Schema.schemaForString.map(str => Uri.parse(str).map(URI.apply).toOption)(_.u.toString())
}
