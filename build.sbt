name         := "monitoring-manager"
version      := "dev"
scalaVersion := "2.13.6"
libraryDependencies ++= Dependencies.all
Compile / packageBin / mainClass := Some("io.hydrosphere.monitoring.manager.Main")
testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
scalacOptions += "-Ymacro-annotations"
Defaults.itSettings
configs(IntegrationTest)

Compile / PB.targets := Seq(
  scalapb.gen(grpc = true)          -> (Compile / sourceManaged).value / "scalapb",
  scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value / "scalapb"
)

IntegrationTest / fork := true

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

lazy val openapi = TaskKey[Unit]("openapi", "Generates OpenAPI documentation")
openapi := (Compile / runMain).toTask(" io.hydrosphere.monitoring.manager.MkDocs").value

Compile / mainClass := Some("io.hydrosphere.monitoring.manager.Main")

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

dockerBaseImage := "openjdk:11"

addCommandAlias("testAll", ";test;it:test")

addCommandAlias("build", ";openapi;docker:publishLocal")
