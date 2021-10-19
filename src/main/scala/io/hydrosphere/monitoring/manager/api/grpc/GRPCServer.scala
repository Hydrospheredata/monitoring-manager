package io.hydrosphere.monitoring.manager.api.grpc

import io.grpc.netty.NettyServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import io.hydrosphere.monitoring.manager.EndpointConfig
import io.hydrosphere.monitoring.manager.Layers.AppEnv
import scalapb.zio_grpc.{Server, ServerLayer}
import zio.{Has, ZIO, ZManaged}
import zio.logging.log

object GRPCServer {
  val serverLayer = for {
    grpcPortHas <- ZIO.access[Has[EndpointConfig]](_.get.grpcPort).toLayer
    grpcPort = grpcPortHas.get
    builder  = NettyServerBuilder.forPort(grpcPort).addService(ProtoReflectionService.newInstance())
    server <- ServerLayer
      .fromServiceLayer(builder)(
        DataStorageServiceImpl.layer ++ ModelCatalogServiceImpl.layer
      )
      .tap(_ => log.info(s"Starting GRPC server at $grpcPort port"))
  } yield server

  def start: ZManaged[AppEnv with zio.ZEnv, Throwable, Has[Server.Service]] = serverLayer.build
}
