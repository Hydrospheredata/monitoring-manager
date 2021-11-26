package io.hydrosphere.monitoring.manager.util

import io.circe._
import io.getquill.MappedEncoding
import sttp.model.{Uri, UriInterpolator}
import sttp.tapir.{Codec, Schema}
import zio.ZIO

case class URI(u: Uri) extends AnyVal {
  override def toString = u.toString()

  def ==(str: String): Boolean =
    this.toString == str

  def maybeBucketName: Option[String] = u.host
  def bucketName: ZIO[Any, IllegalArgumentException, String] = ZIO
    .fromOption(maybeBucketName)
    .orElseFail(new IllegalArgumentException(s"Can't extract bucket name from $u"))

  def objectPath: String = u.pathSegments.toString
}

object URI {
  implicit class Context(val sc: StringContext) {
    def uri(args: Any*): URI = URI(UriInterpolator.interpolate(sc, args: _*))
  }

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

  implicit val codec = Codec.uri.map(URI.apply _)(_.u)
}
