import sbt._

object Dependencies {
  final val zioVersion        = "1.0.12"
  final val zioAwsVersion     = "3.17.61.1"
  final val zioConfigVersion  = "1.0.10"
  final val log4jVersion      = "2.14.1"
  final val doobieVersion     = "1.0.0-M5"
  final val tapirVersion      = "0.19.0-M7"
  final val quillVersion      = "3.10.0"
  final val zioLogVersion     = "0.5.12"
  final val enumeratumVersion = "1.7.0"
  final val sttpVersion       = "3.3.15"
  final val monocleVersion    = "3.1.0"

  final val api = Seq(
    "io.grpc"                      % "grpc-netty"               % scalapb.compiler.Version.grpcJavaVersion,
    "com.thesamet.scalapb"        %% "scalapb-runtime-grpc"     % scalapb.compiler.Version.scalapbVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-zio-http"           % tapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % tapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"       % tapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion
  )

  final val http = Seq(
    "com.softwaremill.sttp.client3" %% "core"                          % sttpVersion,
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpVersion,
    "com.softwaremill.sttp.client3" %% "circe"                         % sttpVersion
  )

  final val data = Seq(
    "io.github.vigoo" %% "zio-aws-s3"     % zioAwsVersion,
    "io.github.vigoo" %% "zio-aws-netty"  % zioAwsVersion,
    "org.postgresql"   % "postgresql"     % "42.2.24",
    "io.getquill"     %% "quill-zio"      % quillVersion,
    "io.getquill"     %% "quill-jdbc-zio" % quillVersion,
    "org.flywaydb"     % "flyway-core"    % "7.14.0",
    "com.zaxxer"       % "HikariCP"       % "3.4.5"
  )

  final val effect = Seq(
    "dev.zio" %% "zio"         % zioVersion,
    "dev.zio" %% "zio-streams" % zioVersion,
    "dev.zio" %% "zio-macros"  % zioVersion
  )

  final val utils = Seq(
    "dev.zio"                 %% "zio-nio"             % "1.0.0-RC11",
    "eu.timepit"              %% "refined"             % "0.9.27",
    "dev.zio"                 %% "zio-config"          % zioConfigVersion,
    "dev.zio"                 %% "zio-config-magnolia" % zioConfigVersion,
    "dev.zio"                 %% "zio-config-refined"  % zioConfigVersion,
    "dev.zio"                 %% "zio-config-typesafe" % zioConfigVersion,
    "org.apache.logging.log4j" % "log4j-api"           % log4jVersion,
    "org.apache.logging.log4j" % "log4j-core"          % log4jVersion,
    "org.apache.logging.log4j" % "log4j-slf4j-impl"    % log4jVersion,
    "dev.zio"                 %% "zio-logging"         % zioLogVersion,
    "dev.zio"                 %% "zio-logging-slf4j"   % zioLogVersion,
    "com.spotify"              % "docker-client"       % "8.16.0" exclude ("ch.qos.logback", "logback-classic"),
    "com.beachape"            %% "enumeratum"          % enumeratumVersion,
    "com.beachape"            %% "enumeratum-circe"    % enumeratumVersion,
    "com.beachape"            %% "enumeratum-quill"    % enumeratumVersion,
    "dev.optics"              %% "monocle-core"        % monocleVersion,
    "dev.optics"              %% "monocle-macro"       % monocleVersion
  )

  final val test = Seq(
    "dev.zio"      %% "zio-test"                        % zioVersion,
    "dev.zio"      %% "zio-test-sbt"                    % zioVersion,
    "dev.zio"      %% "zio-test-junit"                  % zioVersion,
    "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.39.11"
  ).map(_ % "test,it")

  final val all = effect ++ utils ++ data ++ api ++ http ++ test
}
