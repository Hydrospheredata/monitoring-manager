package io.hydrosphere.monitoring.manager

import io.hydrosphere.monitoring.manager.api.http.{ModelEndpoint, PluginEndpoint, PluginProxyEndpoint, ReportEndpoint}
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import zio.{Chunk, ExitCode, URIO, ZIO}
import zio.config.generateDocs
import zio.nio.core.file.Path
import zio.nio.file.Files
import zio.console._

object MkDocs extends zio.App {
  def mkConfigDocs() = for {
    tables <- ZIO.effect {
      Array(
        generateDocs(Config.awsDesc).toTable.toGithubFlavouredMarkdown,
        generateDocs(Config.endpointDesc).toTable.toGithubFlavouredMarkdown,
        generateDocs(Config.metricDesc).toTable.toGithubFlavouredMarkdown
      )
    }
    _ <- Files.writeLines(Path("docs/config.md"), tables)
  } yield ()

  def generateOpenApi() = ZIO.effect {
    val routes =
      PluginEndpoint.endpoints ++ ModelEndpoint.endpoints ++ PluginProxyEndpoint.endpoints ++ ReportEndpoint.endpoints
    val interpreter = OpenAPIDocsInterpreter()
    interpreter.toOpenAPI(routes, "Monitoring manager", "v1")
  }

  def mkOpenApi() = for {
    openApi <- generateOpenApi()
    yamlStr = openApi.toYaml
    path    = Path("docs/openapi.yaml")
    _ <- Files.writeLines(path, Array(yamlStr))
    _ <- putStrLn(s"Wrote OpenAPI docs to $path")
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = (for {
    _ <- mkOpenApi()
    _ <- mkConfigDocs()
  } yield ()).exitCode
}
