name         := "monitoring-manager"
scalaVersion := "2.13.6"

libraryDependencies ++= Dependencies.all

Compile / packageBin / mainClass := Some("io.hydrosphere.monitoring.manager.Main")

Compile / PB.targets := Seq(
  scalapb.gen(grpc = true)          -> (Compile / sourceManaged).value / "scalapb",
  scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value / "scalapb"
)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
configs(IntegrationTest)
Defaults.itSettings

scalacOptions += "-Ymacro-annotations"
