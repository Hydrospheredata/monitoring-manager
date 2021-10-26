package io.hydrosphere.monitoring.manager.util

import io.github.vigoo.zioaws.s3.model.S3Object
import io.hydrosphere.monitoring.manager.GenericUnitTest
import sttp.client3._
import zio.test._
import zio.test.Assertion.equalTo

object UriUtilSpec extends GenericUnitTest {
  def spec = suite("UriUtil")(
    test("should combine base uri with segment") {
      val base     = uri"localhost:8088/service"
      val postfix  = uri"/plugininfo.json"
      val expected = uri"localhost:8088/service/plugininfo.json"
      assert(UriUtil.combine(base, postfix))(equalTo(expected))
    },
    test("should convert S3 object to full path") {
      val res = UriUtil.s3Path("bucket", "asdasd/qweqwe/qwe.123")
      assert(res)(equalTo(uri"s3://bucket/asdasd/qweqwe/qwe.123"))
    }
  )
}
