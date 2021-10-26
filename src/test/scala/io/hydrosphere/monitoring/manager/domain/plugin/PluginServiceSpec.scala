package io.hydrosphere.monitoring.manager.domain.plugin

import io.hydrosphere.monitoring.manager.{Config, GenericUnitTest}
import io.hydrosphere.monitoring.manager.util.URI
import sttp.client3._
import zio.test._
import zio.test.Assertion._
import zio.test.mock.Expectation._
import io.circe.syntax._

object PluginServiceSpec extends GenericUnitTest {
  def spec = suite("PluginService")(
    test("resolve url") {
      val plugin = Plugin(
        name = "test",
        description = "test",
        pluginInfo = Some(
          PluginInfo(
            addr = URI(uri"localhost:1231"),
            routePath = "asd",
            ngModuleName = "asd",
            remoteEntry = None,
            remoteName = "asd",
            exposedModule = "asd"
          )
        )
      )
      val expected = Plugin(
        name = "test",
        description = "test",
        pluginInfo = Some(
          PluginInfo(
            addr = URI(uri"localhost:1231"),
            routePath = "asd",
            ngModuleName = "asd",
            remoteEntry =
              Some(URI(uri"manager:123/api/v1/plugin-proxy/test/static/remoteEntry.js")),
            remoteName = "asd",
            exposedModule = "asd"
          )
        )
      )
      val result = PluginService.resolveRemoteEntry(plugin, URI(uri"manager:123"))
      print(result.asJson.toString())
      assert(result)(equalTo(expected))
    },
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
          .provideLayer(pluginRepoMock.toLayer ++ Config.layer)
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
            .atLeast(1) &&
            PluginRepository.PluginRepositoryMock
              .Update(equalTo(plugin), value(plugin))
              .atMost(1)
              .atLeast(1)
        val effect = PluginService
          .register(plugin)
          .provideLayer(pluginRepoMock.toLayer ++ Config.layer)
        assertM(effect)(equalTo(plugin))
      }
    )
  )
}
