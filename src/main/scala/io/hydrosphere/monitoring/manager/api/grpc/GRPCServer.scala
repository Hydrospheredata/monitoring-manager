package io.hydrosphere.monitoring.manager.api.grpc

import io.grpc.netty.NettyServerBuilder
import io.hydrosphere.monitoring.manager.EndpointConfig
import io.hydrosphere.monitoring.manager.domain.data.InferenceSubscriptionService
import io.hydrosphere.monitoring.manager.domain.model.{ModelRepository, ModelSubscriptionManager}
import io.hydrosphere.monitoring.manager.Layers.AppEnv
import scalapb.zio_grpc.{Server, ServerLayer}
import zio.{Has, ZIO, ZManaged}
import zio.logging.{log, Logger}

object GRPCServer {
  val serverLayer = for {
    grpcPortHas <- ZIO.access[Has[EndpointConfig]](_.get.grpcPort).toLayer
    grpcPort = grpcPortHas.get
    server <- ServerLayer
      .fromServiceLayer(NettyServerBuilder.forPort(grpcPort))(
        DataStorageServiceImpl.layer ++ ModelCatalogServiceImpl.layer
      )
      .tap(_ => log.info(s"Starting GRPC server at $grpcPort port"))
  } yield server

  def start: ZManaged[AppEnv with zio.ZEnv, Throwable, Has[Server.Service]] = serverLayer.build
}
