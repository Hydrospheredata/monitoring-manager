package io.hydrosphere.monitoring.manager.domain.plugin
//NB(bulat): not actual for POC
//import io.hydrosphere.monitoring.manager.GenericUnitTest
//import io.hydrosphere.monitoring.manager.util.URI
//import sttp.client3._
//import sttp.client3.asynchttpclient.zio._
//import zio.test.assertM
//import zio.test.Assertion.equalTo
//import zio.Has
//
//object PluginInfoSpec extends GenericUnitTest {
//  override def spec = suite("PluginInfo") {
//    testM("should fetch correct PluginInfo from url") {
//      val pluginInfo = PluginInfo(URI(uri"localhost"), "1", "2", "3", "4", "5", "6")
//
//      val backend = AsyncHttpClientZioBackend.stub
//        .whenRequestMatches(_.uri.toString() == "0.0.0.0:8080/plugin/test/plugininfo.json")
//        .thenRespond(Right(pluginInfo))
//      assertM(PluginInfo.getPluginInfo(uri"0.0.0.0:8080/plugin/test"))(equalTo(pluginInfo))
//        .provide(Has(backend))
//    }
//  }
//}
