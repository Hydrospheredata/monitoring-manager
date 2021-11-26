package io.hydrosphere.monitoring.manager.domain.model

import io.hydrosphere.monitoring.manager.domain.data.S3Client
import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}
import zio.ZIO

object ModelService {
  case class ModelAlreadyExists(model: Model)
      extends Error(
        s"Can't register model ${model.name}:${model.version} because there is already one"
      )

  case class ModelNotFound(modelName: ModelName, modelVersion: ModelVersion)
      extends Error(
        s"Can't find model $modelName:$modelVersion"
      )

  case class TrainingDataIsNotAvailable(model: Model)
      extends Error(s"Can't access training data at ${model.trainingDataPrefix}")

  def registerModel(model: Model) =
    for {
      existingModel <- ModelRepository.get(model.name, model.version)
      _ <- existingModel match {
        case Some(value) => ZIO.fail(ModelAlreadyExists(value))
        case None        => ZIO.unit
      }
      _ <- model.trainingDataPrefix match {
        case Some(value) =>
          ZIO
            .fail(TrainingDataIsNotAvailable(model))
            .whenM(S3Client.objectExists(value).map(!_))
        case None => ZIO.unit
      }
      res <- ModelRepository.create(model)
    } yield res

  def findModel(modelName: ModelName, modelVersion: ModelVersion) = for {
    maybeMv <- ModelRepository.get(modelName, modelVersion)
    mv <- maybeMv match {
      case Some(value) => ZIO(value)
      case None        => ZIO.fail(ModelNotFound(modelName, modelVersion))
    }
  } yield mv
}
