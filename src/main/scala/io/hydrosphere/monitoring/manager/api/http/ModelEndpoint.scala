package io.hydrosphere.monitoring.manager.api.http

import io.hydrosphere.monitoring.manager.domain.model._
import zio._

case class ModelEndpoint(
    modelRepo: ModelRepository
) extends GenericEndpoint {

  val modelList = ModelEndpoint.modelListDesc
    .serverLogic[Task](_ =>
      modelRepo
        .all()
        .runCollect
        .either
    )

  val modelAdd =
    ModelEndpoint.modelAddDesc
      .serverLogic[Task](model =>
        ModelService
          .registerModel(model)
          .provide(Has(modelRepo))
          .either
      )

  val serverEndpoints = List(modelList, modelAdd)
}

object ModelEndpoint extends GenericEndpoint {
  val modelEndpoint = v1Endpoint
    .in("model")
    .tag("Model")

  val modelListDesc = modelEndpoint
    .name("modelList")
    .description("List all registered models")
    .get
    .out(jsonBody[Seq[Model]])
    .errorOut(throwableBody)

  val modelAddDesc = modelEndpoint
    .name("modelAdd")
    .description("Register new model")
    .in(jsonBody[Model])
    .post
    .out(jsonBody[Model])
    .errorOut(throwableBody)

  val endpoints = List(modelListDesc, modelAddDesc)

  val layer = (ModelEndpoint.apply _).toLayer
}
