package io.hydrosphere.monitoring.manager.api.http

import sttp.tapir.{EndpointIO, Tapir}
import sttp.tapir.generic.SchemaDerivation
import sttp.tapir.json.circe.TapirJsonCirce
import sttp.tapir.ztapir._

trait GenericEndpoint extends TapirJsonCirce with SchemaDerivation with Tapir with ZTapir {
  val v1Endpoint = endpoint.in("api" / "v1")

  val throwableBody: EndpointIO.Body[String, Throwable] =
    plainBody[String].map[Throwable]((x: String) => new Error(x))(x => x.getMessage)
}
