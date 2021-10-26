package io.hydrosphere.monitoring.manager.domain

import io.github.vigoo.zioaws.s3.model.S3Object
import io.hydrosphere.monitoring.manager.{GenericUnitTest, Layers}
import io.hydrosphere.monitoring.manager.domain.data.{
  InferenceSubscriptionService,
  S3Client,
  S3ObjectIndex
}
import sttp.model.Uri
import zio.stream.ZStream
import sttp.client3._
import zio.logging.log
import zio.test.assertM
import zio.test.Assertion.equalTo
import zio.test.environment.TestClock
import zio.{Chunk, Schedule, ZIO, ZQueue}
import zio.clock.Clock

import java.time.Duration

//object InferenceSubSpec extends GenericUnitTest {
//  override def spec = suite("Inference data subs")(
//    testM("1 Plugin <- 1 Prefix") {
//      val pid     = "plugin-1"
//      val prefix1 = uri"s3://bucket/prefix"
//      val data = Chunk(
//        S3Object(key = Some("1")),
//        S3Object(key = Some("2"))
//      )
//      val expected = List(
//        S3Object(key = Some("1")),
//        S3Object(key = Some("2"))
//      )
//      val s3Client: S3Client = (prefix: Uri) => ZStream.fromChunk(data).map(_.asReadOnly)
//      val f = for {
//        q        <- ZQueue.unbounded[S3Object]
//        index    <- S3ObjectIndex.make()
//        pMan     <- InferenceSubscriptionService.make(s3Client, index)
//        itemsFbr <- pMan.subscribe(pid, prefix1).tap(x => q.offer(x.editable)).runDrain.fork
//        _        <- TestClock.adjust(Duration.ofSeconds(60))
//        _        <- log.info(s"Items fbr ${itemsFbr.id}")
//        _        <- itemsFbr.interrupt
//        itemsE   <- q.takeAll
//      } yield itemsE
//      assertM(f.provideSomeLayer[Clock with TestClock](Layers.logger))(equalTo(data.toList))
//    },
//    testM("1 Plugin <- 2 Prefixes") {
//      val pid     = "plugin2-1"
//      val prefix1 = uri"s3://bucket1/prefix1"
//      val prefix2 = uri"s3://bucket2/prefix2"
//      val data1 = Chunk(
//        S3Object(key = Some("s3://bucket1/prefix1/1")),
//        S3Object(key = Some("s3://bucket1/prefix1/2"))
//      )
//      val data2 = Chunk(
//        S3Object(key = Some("s3://bucket2/prefix2/3")),
//        S3Object(key = Some("s3://bucket2/prefix2/4"))
//      )
//      val expected = List(
//        S3Object(key = Some("s3://bucket1/prefix1/1")),
//        S3Object(key = Some("s3://bucket1/prefix1/2")),
//        S3Object(key = Some("s3://bucket2/prefix2/3")),
//        S3Object(key = Some("s3://bucket2/prefix2/4"))
//      )
//      val s3Client: S3Client = (prefix: Uri) =>
//        prefix.pathSegments.segments.toList.map(_.v) match {
//          case "prefix1" :: _ => ZStream.fromChunk(data1.map(_.asReadOnly))
//          case "prefix2" :: _ => ZStream.fromChunk(data2.map(_.asReadOnly))
//          case x              => ZStream.fail(new Exception(s"Impossible prefix: $x"))
//        }
//      val f = for {
//        q         <- ZQueue.unbounded[S3Object]
//        index     <- S3ObjectIndex.make()
//        pMan      <- InferenceSubscriptionService.make(s3Client, index)
//        itemsFbr1 <- pMan.subscribe(pid, prefix1).tap(x => q.offer(x.editable)).runDrain.fork
//        itemsFbr2 <- pMan.subscribe(pid, prefix2).tap(x => q.offer(x.editable)).runDrain.fork
//        _         <- TestClock.adjust(Duration.ofSeconds(60))
//        _         <- itemsFbr1.interrupt
//        _         <- itemsFbr2.interrupt
//        itemsE    <- q.takeAll
//        _         <- log.info(itemsE.mkString(","))
//      } yield itemsE.flatMap(_.key).toSet
//      assertM(f.provideSomeLayer[Clock with TestClock](Layers.logger))(
//        equalTo(expected.flatMap(_.key).toSet)
//      )
//    }
//  )
//}
