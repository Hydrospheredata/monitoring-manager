package io.hydrosphere.monitoring.manager.domain.report

import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}

object ReportErrors {
  case class InvalidAckReport(pluginId: String, message: String)
      extends Error(s"Invalid ackreport from $pluginId: $message")

  case class ReportNotFound(modelName: ModelName, modelVersion: ModelVersion, inferenceFile: String)
      extends Error(s"Can't find report for $inferenceFile ($modelName:$modelVersion)")
}
