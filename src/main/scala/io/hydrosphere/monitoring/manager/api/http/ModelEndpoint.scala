package io.hydrosphere.monitoring.manager.api.http

import io.hydrosphere.monitoring.manager.domain.model._
import sttp.tapir.ztapir._
import zio._

case class ModelEndpoint(
    modelRepo: ModelRepository
) extends GenericEndpoint {
  val modelEndpoint = v1Endpoint
    .in("model")
    .tag("Model")

  val modelList = modelEndpoint
    .name("modelList")
    .description("List all registered models")
    .get
    .out(jsonBody[Seq[Model]])
    .errorOut(throwableBody)
    .serverLogic[Task](_ =>
      modelRepo
        .all()
        .runCollect
        .either
    )

  val modelAdd =
    modelEndpoint
      .name("modelAdd")
      .description("Register new model")
      .in(jsonBody[Model])
      .post
      .out(jsonBody[Model])
      .errorOut(throwableBody)
      .serverLogic[Task](model =>
        ModelService
          .registerModel(model)
          .provide(Has(modelRepo))
          .either
      )

  val endpoints = List(modelList, modelAdd)
}

object ModelEndpoint {
  val layer = (ModelEndpoint.apply _).toLayer
}
