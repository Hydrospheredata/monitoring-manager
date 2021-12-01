package io.hydrosphere.monitoring.manager.api.grpc

import io.grpc.health.v1.{HealthCheckRequest, HealthCheckResponse, HealthGrpc}
import io.grpc.stub.StreamObserver

final case class HealthCheckServiceImpl() extends HealthGrpc.HealthImplBase {
  override def check(request: HealthCheckRequest, responseObserver: StreamObserver[HealthCheckResponse]): Unit = {
    responseObserver.onNext(HealthCheckResponse.newBuilder().setStatus(HealthCheckResponse.ServingStatus.SERVING).build())
    responseObserver.onCompleted()
  }
}
