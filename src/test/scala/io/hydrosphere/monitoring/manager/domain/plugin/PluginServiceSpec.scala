package io.hydrosphere.monitoring.manager.domain.plugin

import io.hydrosphere.monitoring.manager.GenericUnitTest
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
        val plugin = Plugin(
          name = "name",
          description = "desc",
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
          .register(plugin)
          .provideLayer(pluginRepoMock.toLayer)
        assertM(effect)(equalTo(plugin))
      },
      testM("should update with existing plugin") {
        val plugin = Plugin(
          name = "name",
          description = "desc",
          None
        )
        val pluginRepoMock =
          PluginRepository.PluginRepositoryMock
            .Get(equalTo(plugin.name), value(Some(plugin)))
            .atMost(1)
            .atLeast(1)
        val effect = PluginService
          .register(plugin)
          .provideLayer(pluginRepoMock.toLayer)
        assertM(effect.flip)(equalTo(PluginAlreadyExistsError(plugin.name)))
      }
    )
  )
}
