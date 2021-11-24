package io.hydrosphere.monitoring.manager.api.http

import io.hydrosphere.monitoring.manager.api.http.ModelEndpoint.listAssociatedReportsDesc
import io.hydrosphere.monitoring.manager.api.http.ReportEndpoint.{jsonBody, path, reportEndpoint, throwableBody}
import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}
import io.hydrosphere.monitoring.manager.domain.model._
import io.hydrosphere.monitoring.manager.domain.report.ReportRepository
import io.hydrosphere.monitoring.manager.util.URI
import zio._

case class ModelEndpoint(
    modelRepo: ModelRepository,
    reportRepository: ReportRepository
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

  val listAssociatedReports = listAssociatedReportsDesc.serverLogic[Task] { case (name, version) =>
    reportRepository.peekForModelVersion(name, version).runCollect.either
  }

  val serverEndpoints = List(modelList, modelAdd, listAssociatedReports)
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

  val listAssociatedReportsDesc = modelEndpoint
    .name("listAssociatedReports")
    .in(path[ModelName]("modelName"))
    .in(path[ModelVersion]("modelVersion"))
    .in("reports")
    .get
    .out(jsonBody[Seq[URI]])
    .errorOut(throwableBody)

  val endpoints = List(modelListDesc, modelAddDesc, listAssociatedReportsDesc)

  val layer = (ModelEndpoint.apply _).toLayer
}
