package io.hydrosphere.monitoring.manager.util

import io.circe._
import io.circe.syntax._
import io.getquill.MappedEncoding

object QuillJson {
  def jsonEncoder[T: Encoder] = MappedEncoding[T, String](_.asJson.toString())
  def jsonDecoder[T: Decoder] = MappedEncoding[String, T](x =>
    parser.decode[T](x).fold(err => throw err, identity)
  ) // NB(bulat): unsafe throw here
}
