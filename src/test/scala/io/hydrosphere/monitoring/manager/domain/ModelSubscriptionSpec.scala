package io.hydrosphere.monitoring.manager.domain

import io.hydrosphere.monitoring.manager.GenericUnitTest
import io.hydrosphere.monitoring.manager.domain.contract.Signature
import io.hydrosphere.monitoring.manager.domain.model._
import sttp.client3._
import zio.{ZHub, ZQueue}
import zio.stream.ZStream
import zio.test.{assertM, Assertion}
import zio.test.mock.Expectation
import zio.test.Assertion.equalTo
import zio.test.environment.TestClock

import java.time.Duration

object ModelSubscriptionSpec extends GenericUnitTest {
  def spec = suite("Model subscriptions")(
    testM("should send all previous models to plugin") {
      val pluginId = "test-1"
      val models = Set(
        Model(
          name = "a",
          version = 1,
          signature = Signature(Seq.empty, Seq.empty),
          metadata = Map.empty,
          trainingDataPrefix = Some(URI(uri"s3://bucket/")),
          inferenceDataPrefix = Some(URI(uri"s3://bucket2/"))
        ),
        Model(
          name = "b",
          version = 1,
          signature = Signature(Seq.empty, Seq.empty),
          metadata = Map.empty,
          trainingDataPrefix = Some(URI(uri"s3://bucket3/")),
          inferenceDataPrefix = Some(URI(uri"s3://bucket4/"))
        )
      )
      val repoMockLayer = ModelRepository.ModelRepositoryMock
        .All(Expectation.value(ZStream.fromIterable(models)))
        .toLayer
      val hubLayer = ZHub.unbounded[Model].toLayer
      val manLayer = (repoMockLayer ++ hubLayer) >>> ModelSubscriptionManager.layer
      val collectedModels = for {
        q <- ZQueue.unbounded[Model]
        fbr <- manLayer.build
          .use(man => man.get.subscribe(pluginId).tap(q.offer).runDrain)
          .fork
        _    <- TestClock.adjust(Duration.ofSeconds(40))
        _    <- fbr.interrupt
        data <- q.takeAll
      } yield data.toSet
      assertM(collectedModels)(equalTo(models))
    },
    testM("should send all inserts into ModelRepository") {
      val pluginId = "test-1"
      val models = Set(
        Model(
          name = "a",
          version = 1,
          signature = Signature(Seq.empty, Seq.empty),
          metadata = Map.empty,
          trainingDataPrefix = Some(URI(uri"s3://bucket/")),
          inferenceDataPrefix = Some(URI(uri"s3://bucket2/"))
        ),
        Model(
          name = "b",
          version = 1,
          signature = Signature(Seq.empty, Seq.empty),
          metadata = Map.empty,
          trainingDataPrefix = Some(URI(uri"s3://bucket3/")),
          inferenceDataPrefix = Some(URI(uri"s3://bucket4/"))
        )
      )
      val newModel = Model(
        name = "a",
        version = 2,
        signature = Signature(Seq.empty, Seq.empty),
        metadata = Map.empty,
        trainingDataPrefix = Some(URI(uri"s3://bucket3/")),
        inferenceDataPrefix = Some(URI(uri"s3://bucket4/"))
      )
      val expected = models + newModel
      val repoMockLayer =
        (ModelRepository.ModelRepositoryMock
          .All(Expectation.value(ZStream.fromIterable(models))) &&
          ModelRepository.ModelRepositoryMock
            .Create(Assertion.equalTo(newModel), Expectation.value(newModel))).toLayer
      val hubLayer = ZHub.unbounded[Model].toLayer
      val hubbedRepo = repoMockLayer.zipPar(hubLayer).map { case (repo, hub) =>
        zio.Has(ModelRepository.hubbedRepository(repo.get, hub.get))
      }
      val manLayer = (hubbedRepo ++ hubLayer) >+> ModelSubscriptionManager.layer
      val collectedModels = for {
        q <- ZQueue.unbounded[Model]
        data <- manLayer.build.use { man =>
          for {
            fbr <- man
              .get[ModelSubscriptionManager]
              .subscribe(pluginId)
              .tap(q.offer)
              .runDrain
              .fork
            _    <- TestClock.adjust(Duration.ofSeconds(10))
            _    <- man.get[ModelRepository].create(newModel)
            _    <- TestClock.adjust(Duration.ofSeconds(40))
            _    <- fbr.interrupt
            data <- q.takeAll
          } yield data.toSet
        }
      } yield data
      assertM(collectedModels)(equalTo(expected))
    }
  )
}
