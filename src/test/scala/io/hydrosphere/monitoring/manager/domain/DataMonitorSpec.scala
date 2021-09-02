package io.hydrosphere.monitoring.manager.domain

import io.github.vigoo.zioaws.s3.model.{ListObjectsV2Request, S3Object}
import io.github.vigoo.zioaws.s3.S3.S3Mock
import io.hydrosphere.monitoring.manager.{GenericUnitTest, Layers}
import zio.stream.ZStream
import zio.test._
import zio.test.Assertion._
import zio.test.mock.Expectation._
import zio.Chunk

object DataMonitorSpec extends GenericUnitTest {
  def toS3Obj(name: String) = S3Object(key = Some(name)).asReadOnly

  override def spec = suite("DataMonitor")(
    suite("key parser")(
      testM("should parse object key with prefix ") {
        val url = S3Object(key = Some("prefix/census/1/123.png")).asReadOnly
        val effect = DataMonitor
          .parseObject(Some("prefix"), url)
        assertM(effect)(equalTo(NewModelData(Model("census", 1), "prefix/census/1/123.png")))
      },
      testM("should parse object key without prefix") {
        val url = S3Object(key = Some("census/1/123.png")).asReadOnly
        val effect = DataMonitor
          .parseObject(None, url)
        assertM(effect)(equalTo(NewModelData(Model("census", 1), "census/1/123.png")))
      }
    ),
    suite("monitor")(
      testM("should get data from S3") {
        val s3Request = ListObjectsV2Request(
          bucket = "bucket",
          prefix = Some("prefix")
        )
        val s3Response = ZStream(
          S3Object(key = Some("prefix/census/1/a.txt")),
          S3Object(key = Some("prefix/census/1/b.txt")),
          S3Object(key = Some("prefix/census/1/a.txt")),
          S3Object(key = Some("prefix/census/1/c.txt")),
          S3Object(key = Some("prefix/census/2/a.txt"))
        )
          .map(_.asReadOnly)
        val s3Mock = S3Mock.ListObjectsV2(equalTo(s3Request), value(s3Response))
        val monitor = DataMonitor(
          bucket = "bucket",
          prefix = Some("prefix")
        )
        val effect = monitor.start
          .provideLayer(s3Mock.toLayer ++ Layers.logger)
          .runCollect
        val expected = Chunk(
          NewModelData(Model("census", 1), "prefix/census/1/a.txt"),
          NewModelData(Model("census", 1), "prefix/census/1/b.txt"),
          NewModelData(Model("census", 1), "prefix/census/1/c.txt"),
          NewModelData(Model("census", 2), "prefix/census/2/a.txt")
        )
        assertM(effect)(equalTo(expected))
      }
    )
  )
}
