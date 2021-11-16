package io.hydrosphere.monitoring.manager.api.http

import io.hydrosphere.monitoring.manager.api.http.ReportEndpoint.{getReportDesc, listReportedFilesDesc}
import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}
import io.hydrosphere.monitoring.manager.domain.report.{Report, ReportRepository}

class ReportEndpoint(reportRepo: ReportRepository) extends GenericEndpoint {
  val listReportedFiles = listReportedFilesDesc.serverLogic { case (name, version) => ??? }

  val getReport = getReportDesc.serverLogic { case (name, version, file) => ??? }
}

object ReportEndpoint extends GenericEndpoint {
  val reportEndpoint = v1Endpoint
    .in("report")
    .tag("Report")

  val listReportedFilesDesc = reportEndpoint
    .in(path[ModelName]("modelName"))
    .in(path[ModelVersion]("modelVersion"))
    .get
    .out(jsonBody[List[String]])

  val getReportDesc = reportEndpoint
    .in(path[ModelName]("modelName"))
    .in(path[ModelVersion]("modelVersion"))
    .in(query[String]("filename"))
    .get
    .out(jsonBody[Report])

  val endpoints = List(listReportedFilesDesc, getReportDesc)
}
