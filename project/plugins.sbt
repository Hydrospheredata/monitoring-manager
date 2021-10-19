addSbtPlugin("com.thesamet"   % "sbt-protoc"          % "1.0.3")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.6")
libraryDependencies ++= Seq(
  "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % "0.5.1"
)
