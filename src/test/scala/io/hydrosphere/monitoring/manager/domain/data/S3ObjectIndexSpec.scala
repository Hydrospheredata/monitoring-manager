package io.hydrosphere.monitoring.manager.domain.data

import io.hydrosphere.monitoring.manager.domain.data.S3ObjectIndex.IndexKey
import io.hydrosphere.monitoring.manager.util.URI.Context
import io.hydrosphere.monitoring.manager.util.ZDeadline
import io.hydrosphere.monitoring.manager.{GenericUnitTest, Layers}
import zio.logging.Logger
import zio.test.environment.TestClock
import zio.test.{assertM, Assertion}
import zio.{clock, Has, ZIO, ZRef}

import java.time.Instant
import scala.concurrent.duration._
import scala.jdk.DurationConverters._

object S3ObjectIndexSpec extends GenericUnitTest {
  val instantL = clock.instant.toLayer
  val data     = instantL.map(i => Has(IndexKey("plugin-1", uri"s3://test/obj", i.get)))
  val index = data.flatMap { s =>
    (for {
      deadline <- ZDeadline.now
      state    <- ZRef.make(Map(s.get -> (deadline + 5.minutes)))
    } yield S3ObjectIndexImpl(state): S3ObjectIndex).toLayer
  }

  val spec = suite("S3ObjectIndex")(
    testM("should identify seen objects") {
      val p = for {
        key <- ZIO.service[IndexKey]
        res <- S3ObjectIndex.isNew(key.pluginId, S3Ref(key.s3Uri, key.s3ModifiedAt))
      } yield res
      assertM(p)(Assertion.isFalse)
    },
    testM("should identify new objects") {
      val newObj = S3Ref(uri"s3://newnewnew", Instant.now())
      val res    = S3ObjectIndex.isNew("plugin-1", newObj)
      assertM(res)(Assertion.isTrue)
    },
    testM("should identify seen objects with overdue deadline") {
      val p = for {
        _   <- TestClock.adjust(6.minutes.toJava)
        key <- ZIO.service[IndexKey]
        res <- S3ObjectIndex.isNew(key.pluginId, S3Ref(key.s3Uri, key.s3ModifiedAt))
      } yield res
      assertM(p)(Assertion.isTrue)
    }
  ).provideCustomLayer(index ++ data)

}
