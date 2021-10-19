package io.hydrosphere.monitoring.manager.api.http

import sttp.tapir.ztapir.plainBody
import sttp.tapir.{endpoint, EndpointIO}
import sttp.tapir.generic.SchemaDerivation
import sttp.tapir.json.circe.TapirJsonCirce
import sttp.tapir.ztapir._

trait GenericEndpoint extends TapirJsonCirce with SchemaDerivation {
  val v1Endpoint = endpoint.in("api" / "v1")

  val throwableBody: EndpointIO.Body[String, Throwable] =
    plainBody[String].map[Throwable]((x: String) => new Error(x))(x => x.getMessage)
}
