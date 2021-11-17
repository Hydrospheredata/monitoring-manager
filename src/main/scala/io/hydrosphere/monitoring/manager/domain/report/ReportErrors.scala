package io.hydrosphere.monitoring.manager.domain.report

import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}

object ReportErrors {
  case class ReportNotFound(modelName: ModelName, modelVersion: ModelVersion, inferenceFile: String)
      extends Error(s"Can't find report for $inferenceFile ($modelName:$modelVersion)")
}
