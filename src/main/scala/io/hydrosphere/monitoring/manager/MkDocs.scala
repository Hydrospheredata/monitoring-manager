package io.hydrosphere.monitoring.manager

import io.hydrosphere.monitoring.manager.api.http.{
  ModelEndpoint,
  PluginEndpoint,
  PluginProxyEndpoint
}
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import zio.ZIO
import zio.nio.core.file.Path
import zio.nio.file.Files
import zio.console._

object MkDocs extends zio.App {
  def generateOpenApi() = ZIO.effect {
    val routes =
      PluginEndpoint.endpoints ++ ModelEndpoint.endpoints ++ PluginProxyEndpoint.endpoints
    val interpreter = OpenAPIDocsInterpreter()
    interpreter.toOpenAPI(routes, "Monitoring manager", "v1")
  }

  override def run(args: List[String]) = (for {
    openApi <- generateOpenApi()
    yamlStr = openApi.toYaml
    path    = Path("openapi.yaml")
    _ <- Files.writeLines(path, Array(yamlStr))
    _ <- putStrLn(s"Wrote OpenAPI docs to $path")
  } yield ()).exitCode
}
