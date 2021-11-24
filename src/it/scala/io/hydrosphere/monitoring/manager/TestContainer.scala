package io.hydrosphere.monitoring.manager

import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import zio.ZManaged
import zio.blocking._
import zio._

object TestContainer {
  val postgres =
    ZManaged.make {
      effectBlocking {
        val container = new PostgreSQLContainer()
        container.start()
        container
      }.orDie
    }(container => effectBlocking(container.stop()).orDie).toLayer
}
