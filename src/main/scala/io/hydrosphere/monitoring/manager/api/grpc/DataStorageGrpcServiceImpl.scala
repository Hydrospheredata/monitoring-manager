package io.hydrosphere.monitoring.manager.api.grpc

import io.grpc.Status
import monitoring_manager.monitoring_manager.ZioMonitoringManager.DataStorageService
import monitoring_manager.monitoring_manager._
import zio.ZIO
import zio.stream.ZStream

//NB(bulat): Extend DataStorageService because we don't need extra GRPC context. Yet.
final class DataStorageGrpcServiceImpl() extends DataStorageService {
  override def getDataUpdates(
      request: GetDataUpdatesRequest
  ): ZStream[Any, Status, GetDataUpdatesResponse] = ???

  override def getModelFiles(
      request: GetModelFilesRequest
  ): ZIO[Any, Status, GetModelFilesResponse] = ???
}

object DataStorageGrpcServiceImpl {
  def layer = ZIO.succeed(new DataStorageGrpcServiceImpl()).toLayer
}
