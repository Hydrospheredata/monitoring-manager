package io.hydrosphere.monitoring.manager

import com.dimafeng.testcontainers.{GenericContainer, PostgreSQLContainer}
import org.testcontainers.containers.BindMode
import zio.{ZIO, ZManaged}
import zio.blocking._

object TestContainer {
  val postgres =
    ZManaged.make {
      effectBlocking {
        val container = new PostgreSQLContainer()
        container.start()
        container
      }.orDie
    }(container => effectBlocking(container.stop()).orDie).toLayer

  val prometheus =
    ZManaged.make {
      effectBlocking {
        val container = GenericContainer(
          dockerImage = "prom/prometheus:v2.31.1",
          exposedPorts = Seq(9090),
          command = Seq("--config.file=/etc/prometheus/config.yaml", "--enable-feature=remote-write-receiver"),
          classpathResourceMapping = Seq(("./prometheus/", "/etc/prometheus/", BindMode.READ_WRITE))
        )
        container.start()
        container
      }.orDie
    }(container => effectBlocking(container.stop()).orDie).toLayer
}
