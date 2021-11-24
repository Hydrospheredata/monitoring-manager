package io.hydrosphere.monitoring.manager

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.hydrosphere.monitoring.manager.db.{CloseableDataSource, FlywayClient}
import zio.blocking.Blocking
import zio.test.TestAspect
import zio.test.TestAspect.{before, beforeAll}
import zio.{Has, ZIO, ZLayer}

object MigrationAspects {
  val dsLayer = (for {
    pg <- ZIO.service[PostgreSQLContainer]
  } yield {
    val config = new HikariConfig()
    config.setJdbcUrl(pg.container.getJdbcUrl)
    config.setUsername(pg.container.getUsername)
    config.setPassword(pg.container.getPassword)
    config.setSchema("hydrosphere")
    new HikariDataSource(config): CloseableDataSource
  }).toLayer

  val flywayLayer = {
    dsLayer >>> FlywayClient.layer
  }

  val pgLayer: ZLayer[Any, Nothing, Has[PostgreSQLContainer]] = Blocking.live >>> TestContainer.postgres

  def migrate(): TestAspect[Nothing, Blocking with Has[PostgreSQLContainer], Nothing, Any] = {
    val migration = FlywayClient.migrate().provideLayer(ZLayer.identity[Blocking] ++ flywayLayer)
    beforeAll(migration.orDie)
  }
}
