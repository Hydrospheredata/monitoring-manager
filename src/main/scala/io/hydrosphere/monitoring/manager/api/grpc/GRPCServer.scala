package io.hydrosphere.monitoring.manager.api.grpc

import io.grpc.netty.NettyServerBuilder
import io.hydrosphere.monitoring.manager.EndpointConfig
import scalapb.zio_grpc.ServerLayer
import zio.{Has, ZIO}
import zio.logging.log

object GRPCServer {
  def serverLayer = for {
    grpcPortHas <- ZIO.access[Has[EndpointConfig]](_.get.grpcPort).toLayer
    grpcPort = grpcPortHas.get
    server <- ServerLayer
      .fromServiceLayer(NettyServerBuilder.forPort(grpcPort))(
        DataStorageGrpcServiceImpl.layer
      )
      .tap(_ => log.info(s"Starting GRPC server at $grpcPort port"))
  } yield server

  def start = serverLayer.build
}
