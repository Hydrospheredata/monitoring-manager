//package io.hydrosphere.monitoring.manager.domain
//
//import io.hydrosphere.monitoring.manager.domain.metrics.MetricsService
//import io.hydrosphere.monitoring.manager.domain.metrics.sender.{MetricSender, RemoteWriteClient}
//import io.hydrosphere.monitoring.manager.util.URI.Context
//import io.hydrosphere.monitoring.manager.{GenericIntegrationTest, Layers, TestContainer}
//import io.prometheus.client.CollectorRegistry
//import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
//import zio.metrics.prometheus.Gauge
//import zio.test.{assert, assertM, Assertion}
//import zio.{Has, ZEnv}
//
//object MetricSenderSpec extends GenericIntegrationTest {
//  val dp = for {
//    prom <- TestContainer.prometheus
//    promContainer = prom.get
////    url = uri"http://${promContainer.host}:${promContainer.mappedPort(9090)}"
//    url = uri"http://localhost:9092"
//    log    <- Layers.logger
//    client <- AsyncHttpClientZioBackend().toLayer
//  } yield Has(RemoteWriteClient(url, client.get, log.get): MetricSender)
//
//  val spec = suite("DirectPusher")(
//    testM("should send metrics directly to Prometheus") {
//      val metric1 = Gauge("hydro_a_counts", Array("foo1", "foo2")) >>= (g => g.set(9, Array("bar1", "bar2")))
//      val prog    = metric1 *> MetricSender.pushEnv("test")
//      assertM(prog)(Assertion.anything).provideSomeLayer[ZEnv](dp ++ MetricsService.emptyCollector)
//    },
//    test("opentelemetry") {
//      assert(true)(Assertion.anything)
//    }
//  )
//}
