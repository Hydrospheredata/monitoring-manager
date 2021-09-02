package io.hydrosphere.monitoring.manager.domain.clouddriver

import sttp.client3._
import zio._
import zio.macros.accessible
import zio.test.mock.mockable

@accessible
trait CloudDriver {
  def createService(
      name: String,
      image: String,
      deploymentConfigName: String
  ): Task[CloudInstance]
}

object CloudDriver {
  @mockable[CloudDriver]
  object CloudDriverMock
}

case class CloudDriverImpl() extends CloudDriver {
  override def createService(
      name: String,
      image: String,
      deploymentConfigName: String
  ): Task[CloudInstance] = ZIO.succeed {
    CloudInstance(name, image, deploymentConfigName, uri = uri"http://localhost:8088")
  }
}

object CloudDriverImpl {
  def layer: URLayer[Any, Has[CloudDriver]] = (CloudDriverImpl.apply _).toLayer
}
