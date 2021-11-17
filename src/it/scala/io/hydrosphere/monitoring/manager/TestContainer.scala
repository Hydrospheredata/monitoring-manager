package io.hydrosphere.monitoring.manager

import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import zio.ZManaged
import zio.blocking._
import zio._

object TestContainer {
  def postgres(imageName: Option[String] = None) =
    ZManaged.make {
      effectBlocking {
        val image = imageName.map(DockerImageName.parse)
        val container = new PostgreSQLContainer(
          dockerImageNameOverride = image
        )
        container.start()
        container
      }.orDie
    }(container => effectBlocking(container.stop()).orDie).toLayer
}
