package io.hydrosphere.monitoring.manager.domain.plugin

import io.hydrosphere.monitoring.manager.GenericUnitTest
import io.hydrosphere.monitoring.manager.api.http.CreatePluginRequest
import io.hydrosphere.monitoring.manager.domain.clouddriver.{CloudDriver, CloudInstance}
import io.hydrosphere.monitoring.manager.domain.plugin.PluginService.PluginAlreadyExistsError
import sttp.client3._
import sttp.client3.asynchttpclient.zio.{AsyncHttpClientZioBackend, SttpClient}
import zio.test._
import zio.test.Assertion._
import zio.test.mock.Expectation._
import zio.Has

object PluginServiceSpec extends GenericUnitTest {
  def spec = suite("PluginService")(
    suite("register")(
      testM("should register new plugin") {
        val pluginRequest = CreatePluginRequest(
          name = "name",
          image = "image",
          depConfigName = "dc",
          description = "desc"
        )
        val plugin = Plugin(
          name = "name",
          image = "image",
          depConfigName = "dc",
          description = "desc",
          status = Plugin.Status.Inactive,
          None
        )
        val pluginRepoMock =
          PluginRepository.PluginRepositoryMock
            .Get(equalTo(plugin.name), value(None))
            .atMost(1)
            .atLeast(1) &&
            PluginRepository.PluginRepositoryMock
              .Insert(equalTo(plugin), value(plugin))
              .atMost(1)
              .atLeast(1)
        val effect = PluginService
          .register(pluginRequest)
          .provideLayer(pluginRepoMock.toLayer)
        assertM(effect)(equalTo(plugin))
      },
      testM("should fail with existing plugin") {
        val pluginRequest = CreatePluginRequest(
          name = "name",
          image = "image",
          depConfigName = "dc",
          description = "desc"
        )
        val plugin = Plugin(
          name = "name",
          image = "image",
          depConfigName = "dc",
          description = "desc",
          status = Plugin.Status.Inactive,
          None
        )
        val pluginRepoMock =
          PluginRepository.PluginRepositoryMock
            .Get(equalTo(plugin.name), value(Some(plugin)))
            .atMost(1)
            .atLeast(1)
        val effect = PluginService
          .register(pluginRequest)
          .provideLayer(pluginRepoMock.toLayer)
        assertM(effect.flip)(equalTo(PluginAlreadyExistsError(plugin.name)))
      }
    ),
    suite("activate")(
      testM("should activate a plugin") {
        val plugin = Plugin(
          name = "name",
          image = "image",
          depConfigName = "dc",
          description = "desc",
          status = Plugin.Status.Inactive,
          None
        )
        val pluginInfo = PluginInfo("1", "2", "3", "4", "5", "6")

        val expected = Plugin(
          name = "name",
          image = "image",
          depConfigName = "dc",
          description = "desc",
          status = Plugin.Status.Active,
          pluginInfo = Some(pluginInfo)
        )

        val pluginRepoMock =
          PluginRepository.PluginRepositoryMock
            .Get(equalTo("plugin-name"), value(Some(plugin)))
            .atLeast(1) &&
            PluginRepository.PluginRepositoryMock
              .Update(equalTo(expected), value(expected))
              .atLeast(1)
        val cdMock = CloudDriver.CloudDriverMock
          .CreateService(
            equalTo(("name", "image", "dc")),
            value(CloudInstance("name", "image", "dc", uri"localhost:8080/plugin/test"))
          )
          .atLeast(1)
        val sttpMock = AsyncHttpClientZioBackend.stub
          .whenRequestMatches(_.uri.toString() == "localhost:8080/plugin/test/plugininfo.json")
          .thenRespond(Right(pluginInfo))

        val layers =
          pluginRepoMock.toLayer ++ cdMock.toLayer
        val effect = PluginService
          .activate("plugin-name")
          .provideSomeLayer[SttpClient](layers)
          .provide(Has(sttpMock))
        assertM(effect)(equalTo(expected))
      }
    )
  )
}
