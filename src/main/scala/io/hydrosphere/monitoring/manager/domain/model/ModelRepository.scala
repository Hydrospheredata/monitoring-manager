package io.hydrosphere.monitoring.manager.domain.model

import io.hydrosphere.monitoring.manager.db.{CloseableDataSource, DatabaseContext}
import io.hydrosphere.monitoring.manager.domain.model.Model.{ModelName, ModelVersion}
import zio._
import zio.macros.accessible
import zio.stream.Stream
import zio.test.mock.Mock
import io.getquill.context.ZioJdbc._
import io.hydrosphere.monitoring.manager.util.QuillJson
import sttp.model.Uri

import java.sql.SQLException

@accessible
trait ModelRepository {
  def create(model: Model): Task[Model]
  def get(name: ModelName, version: ModelVersion): Task[Option[Model]]
  def all(): Stream[Throwable, Model]
}

object ModelRepository {
  def hubbedRepository(mr: ModelRepository, hub: zio.Hub[Model]) = new ModelRepository {
    override def create(model: Model) =
      mr.create(model)
        .tap(hub.publish) //NB(bulat): Can block if hub is full. Needs to be configured properly.

    override def get(name: ModelName, version: ModelVersion) = mr.get(name, version)

    override def all() = mr.all()
  }

  object ModelRepositoryMock extends Mock[Has[ModelRepository]] {
    case object Create extends Method[Model, Throwable, Model]
    case object Get    extends Method[(ModelName, ModelVersion), Throwable, Option[Model]]
    case object All    extends Stream[Unit, Throwable, Model]

    override val compose = ZLayer.fromServiceM { proxy =>
      withRuntime.map { runtime =>
        new ModelRepository {
          override def create(model: Model) = proxy(Create, model)

          override def get(name: ModelName, version: ModelVersion) = proxy(Get, name, version)

          override def all() = runtime.unsafeRun(proxy(All))
        }
      }
    }
  }
}

final case class ModelRepositoryImpl(
    dataSource: CloseableDataSource,
    ctx: DatabaseContext
) extends ModelRepository {
  import ctx._
  final private val hasDS = Has(dataSource)

  implicit private val mapDecoder = QuillJson.jsonDecoder[Map[String, String]]
  implicit private val mapEncoder = QuillJson.jsonEncoder[Map[String, String]]

  override def create(model: Model) = ctx
    .run(
      quote(query[Model].insert(lift(model)))
    )
    .onDataSource
    .provide(hasDS)
    .as(model)

  override def get(name: ModelName, version: ModelVersion) =
    ctx
      .run(quote(query[Model].filter(m => m.name == lift(name) && m.version == lift(version))))
      .map(_.headOption)
      .onDataSource
      .provide(hasDS)

  override def all() = ctx
    .stream(quote(query[Model]))
    .provideLayer(DataSourceLayer.live)
    .refineToOrDie[SQLException]
    .provide(hasDS)
}

object ModelRepositoryImpl {
  val layer = (
    for {
      ds  <- ZIO.service[CloseableDataSource]
      ctx <- ZIO.service[DatabaseContext]
      hub <- ZIO.service[Hub[Model]]
      repo   = ModelRepositoryImpl(ds, ctx)
      hubbed = ModelRepository.hubbedRepository(repo, hub)
    } yield hubbed
  ).toLayer
}
