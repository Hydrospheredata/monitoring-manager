package io.hydrosphere.monitoring.manager.api.grpc

import io.grpc.netty.NettyServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import io.hydrosphere.monitoring.manager.EndpointConfig
import scalapb.zio_grpc.ServerLayer
import zio.{Has, ZIO, ZLayer}
import zio.logging.log

object GRPCServer {
  val serverLayer = for {
    grpcPortHas <- ZLayer
      .service[EndpointConfig]
    grpcPort = grpcPortHas.get.grpcPort
    builder  = NettyServerBuilder.forPort(grpcPort).addService(ProtoReflectionService.newInstance())
    dataStorage   <- DataStorageServiceImpl.layer
    modelCatalog  <- ModelCatalogServiceImpl.layer
    pluginManager <- PluginManagementServiceImpl.layer
    server <- ServerLayer
      .fromServices(builder, dataStorage.get, modelCatalog.get, pluginManager.get)
      .tap(_ => log.info(s"Starting GRPC server at $grpcPort port"))
  } yield server

  def start = serverLayer.build
}
