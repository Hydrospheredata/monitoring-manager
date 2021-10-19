package io.hydrosphere.monitoring.manager.domain.plugin

import io.getquill.context.ZioJdbc._
import io.hydrosphere.monitoring.manager.db.{CloseableDataSource, DatabaseContext}
import zio._
import zio.macros.accessible
import zio.stream.Stream
import zio.test.mock.Mock

import java.sql.SQLException

@accessible
trait PluginRepository {
  def insert(plugin: Plugin): Task[Plugin]

  def all(): Stream[Throwable, Plugin]

  def get(name: String): Task[Option[Plugin]]

  def update(plugin: Plugin): Task[Plugin]
}

object PluginRepository {
  object PluginRepositoryMock extends Mock[Has[PluginRepository]] {
    case object Insert extends Method[Plugin, Throwable, Plugin]
    case object All    extends Stream[Unit, Throwable, Plugin]
    case object Get    extends Method[String, Throwable, Option[Plugin]]
    case object Update extends Method[Plugin, Throwable, Plugin]
    override val compose = ZLayer.fromServiceM { proxy =>
      withRuntime.map { runtime =>
        new PluginRepository {
          override def insert(plugin: Plugin) = proxy(Insert, plugin)

          override def all() = runtime.unsafeRun(proxy(All))

          override def get(name: String) = proxy(Get, name)

          override def update(plugin: Plugin) = proxy(Update, plugin)
        }
      }
    }
  }
}

final case class PluginRepositoryImpl(
    dataSource: CloseableDataSource,
    ctx: DatabaseContext
) extends PluginRepository {
  import ctx._
  final private val hasDS = Has(dataSource)

  override def insert(plugin: Plugin) =
    ctx
      .run(quote(query[Plugin].insert(lift(plugin))))
      .onDataSource
      .as(plugin)
      .provide(hasDS)

  override def all() =
    ctx
      .stream(quote(query[Plugin]))
      .provideLayer(DataSourceLayer.live)
      .refineToOrDie[SQLException]
      .provide(hasDS)

  override def get(name: String) =
    ctx
      .run(quote(query[Plugin].filter(_.name == lift(name))))
      .map(_.headOption)
      .onDataSource
      .provide(hasDS)

  override def update(plugin: Plugin) = {
    val q = quote(query[Plugin].filter(_.name == lift(plugin.name)).update(lift(plugin)))
    ctx
      .run(q)
      .onDataSource
      .provide(hasDS)
      .as(plugin)
  }
}

object PluginRepositoryImpl {
  val layer: URLayer[Has[CloseableDataSource] with Has[DatabaseContext], Has[PluginRepository]] =
    (PluginRepositoryImpl(_, _)).toLayer
}
