package io.hydrosphere.monitoring.manager

import io.github.vigoo.zioaws.s3
import io.github.vigoo.zioaws.s3.model.{ListObjectsV2Request, S3Object}
import io.hydrosphere.monitoring.manager.domain.{DataMonitor, Model, NewModelData}
import zio.Chunk
import zio.test._
import zio.test.Assertion._

object DataMonitorITSpec extends GenericIntegrationTest {
  override def spec = suite("DataMonitorITSpec") {
    testM("should list minio objects") {
      val config = Layers.aws
      val mon    = DataMonitor("test-bucket", Some("prefix"))
      val effect = mon.start
        .provideLayer(config ++ Layers.logger)
        .runCollect
//      val request = ListObjectsV2Request(
//        bucket = "test-bucket",
//        prefix = None
//      )
//      val effect = s3
//        .listObjectsV2(request)
//        .mapM(_.key)
//        .provideLayer(config)
//        .runCollect
      val expected = Chunk(NewModelData(Model("census", 1), "prefix/census/1/123.png"))
      assertM(effect)(equalTo(expected))
    }
  }
}
