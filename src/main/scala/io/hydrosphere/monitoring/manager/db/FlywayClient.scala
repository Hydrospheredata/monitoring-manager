package io.hydrosphere.monitoring.manager.db

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import zio.{Has, Task, ZIO, ZLayer}

import javax.sql.DataSource

/** Effectful wrapper for Java Flyway Client
  */
trait FlywayClient {
  def migrate(): Task[MigrateResult]
}

object FlywayClient {
  def wrap(fl: Flyway): FlywayClient = () => ZIO.effect(fl.migrate())

  def makeJavaClient(dataSource: DataSource): Task[Flyway] =
    ZIO.effect(Flyway.configure().dataSource(dataSource).schemas("hydrosphere").load())

  val layer: ZLayer[Has[CloseableDataSource], Throwable, Has[FlywayClient]] =
    ZIO
      .environment[Has[CloseableDataSource]]
      .flatMap(x => makeJavaClient(x.get[CloseableDataSource]))
      .map(FlywayClient.wrap)
      .toLayer
}
