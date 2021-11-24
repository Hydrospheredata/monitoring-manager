package io.hydrosphere.monitoring.manager.api.http

import io.hydrosphere.monitoring.manager.api.http.ReportEndpoint.getReportDesc
import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}
import io.hydrosphere.monitoring.manager.domain.report.{Report, ReportRepository}
import io.hydrosphere.monitoring.manager.util.URI
import zio._

case class ReportEndpoint(reportRepo: ReportRepository) extends GenericEndpoint {

  val getReport = getReportDesc.serverLogic[Task] { case (name, version, file) =>
    reportRepo.get(name, version, file).runCollect.either
  }

  val serverEndpoints = List(getReport)
}

object ReportEndpoint extends GenericEndpoint {
  val layer = (ReportEndpoint.apply _).toLayer

  val reportEndpoint = v1Endpoint
    .in("report")
    .tag("Report")

  val getReportDesc = reportEndpoint
    .name("getReportDesc")
    .in(query[ModelName]("modelName"))
    .in(query[ModelVersion]("modelVersion"))
    .in(query[URI]("file"))
    .get
    .out(jsonBody[Seq[Report]])
    .errorOut(throwableBody)

  val endpoints = List(getReportDesc)
}
