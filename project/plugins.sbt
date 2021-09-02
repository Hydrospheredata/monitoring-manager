addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.3")

libraryDependencies ++= Seq(
  "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % "0.5.1"
)
