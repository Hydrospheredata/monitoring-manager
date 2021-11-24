package io.hydrosphere.monitoring.manager.db

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.output.MigrateResult
import zio._
import zio.blocking.Blocking
import zio.macros.accessible

/** Effectful wrapper for Java Flyway Client
  */
@accessible
trait FlywayClient {
  def migrate(): ZIO[Blocking, FlywayException, MigrateResult]
}

object FlywayClient {
  def wrap(fl: Flyway): FlywayClient = new FlywayClient {
    override def migrate() = blocking.blocking(ZIO.effect(fl.migrate())).mapError(_.asInstanceOf[FlywayException])
  }

  val javaClient: ZLayer[Has[CloseableDataSource], Throwable, Has[Flyway]] = ({
    for {
      ds     <- ZIO.service[CloseableDataSource]
      client <- ZIO.effect(Flyway.configure().dataSource(ds).schemas("hydrosphere").load())
    } yield client
  }).toLayer

  val wrapperLayer: URLayer[Has[Flyway], Has[FlywayClient]] = (FlywayClient.wrap _).toLayer

  val layer = javaClient >>> wrapperLayer
}
