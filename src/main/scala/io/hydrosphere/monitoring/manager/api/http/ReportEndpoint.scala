package io.hydrosphere.monitoring.manager.api.http

import io.hydrosphere.monitoring.manager.api.http.ReportEndpoint.{getReportDesc, listReportedFilesDesc, reportEndpoint}
import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}
import io.hydrosphere.monitoring.manager.domain.report.{Report, ReportRepository}
import zio._

case class ReportEndpoint(reportRepo: ReportRepository) extends GenericEndpoint {
  val listReportedFiles = listReportedFilesDesc.serverLogic[Task] { case (name, version) =>
    reportRepo.peekForModelVersion(name, version).runCollect.either
  }

  val getReport = getReportDesc.serverLogic[Task] { case (name, version, file) =>
    reportRepo.get(name, version, file).runCollect.either
  }

  val serverEndpoints = List(listReportedFiles, getReport)
}

object ReportEndpoint extends GenericEndpoint {
  val layer = (ReportEndpoint.apply _).toLayer

  val reportEndpoint = v1Endpoint
    .in("report")
    .tag("Report")

  val listReportedFilesDesc = reportEndpoint
    .name("listReportedFilesDesc")
    .in(path[ModelName]("modelName"))
    .in(path[ModelVersion]("modelVersion"))
    .get
    .out(jsonBody[Seq[String]])
    .errorOut(throwableBody)

  val getReportDesc = reportEndpoint
    .name("getReportDesc")
    .in(path[ModelName]("modelName"))
    .in(path[ModelVersion]("modelVersion"))
    .in(path[String]("filename"))
    .get
    .out(jsonBody[Seq[Report]])
    .errorOut(throwableBody)

  val endpoints = List(listReportedFilesDesc, getReportDesc)
}
