package io.hydrosphere.monitoring.manager.domain.plugin

import io.getquill.context.ZioJdbc._
import io.hydrosphere.monitoring.manager.db.{CloseableDataSource, DatabaseContext}
import zio._
import zio.macros.accessible
import zio.test.mock.mockable

@accessible
trait PluginRepository {
  def insert(plugin: Plugin): Task[Plugin]

  def list(): Task[Seq[Plugin]]

  def get(name: String): Task[Option[Plugin]]

  def update(plugin: Plugin): Task[Plugin]
}

object PluginRepository {
  @mockable[PluginRepository]
  object PluginRepositoryMock
}

final case class PluginRepositoryImpl(
    dataSource: CloseableDataSource,
    ctx: DatabaseContext
) extends PluginRepository {
  import ctx._
  final private val hasDS = Has(dataSource)

  override def insert(plugin: Plugin): Task[Plugin] =
    ctx
      .run(quote(query[Plugin].insert(lift(plugin))))
      .onDataSource
      .as(plugin)
      .provide(hasDS)

  override def list(): Task[Seq[Plugin]] =
    ctx
      .run(quote(query[Plugin]))
      .onDataSource
      .provide(hasDS)

  override def get(name: String): Task[Option[Plugin]] =
    ctx
      .run(quote(query[Plugin].filter(_.name == lift(name))))
      .map(_.headOption)
      .onDataSource
      .provide(hasDS)

  override def update(plugin: Plugin): Task[Plugin] = {
    val q = quote(query[Plugin].filter(_.name == lift(plugin.name)).update(lift(plugin)))
    ctx
      .run(q)
      .onDataSource
      .provide(hasDS)
      .as(plugin)
  }
}

object PluginRepositoryImpl {
  def layer: URLayer[Has[CloseableDataSource] with Has[DatabaseContext], Has[PluginRepository]] =
    (PluginRepositoryImpl(_, _)).toLayer
}
