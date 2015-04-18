import com.typesafe.sbt.web.Import.WebKeys._

import com.typesafe.sbt.packager.docker._

name := """scala-play-sample"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.typesafe.play" %% "play-slick" % "0.8.0",
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "mysql" % "mysql-connector-java" % "5.1.34",
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "org.webjars" % "jquery" % "1.11.1",
  "org.mockito" % "mockito-all" % "1.10.8" % "test",
  "com.googlecode.jmockit" % "jmockit" % "1.7" % "test",
  "org.webjars" % "mocha" % "2.0.1" % "test",
  "org.webjars" % "chai" % "1.9.1" % "test",
  "org.webjars" % "sinonjs" % "1.7.3" % "test"
)

webModuleDirectory in TestAssets := webTarget.value / "node-modules" / "test"

scalariformSettings

javaOptions in Test += "-Dconfig.resource=test.conf"

maintainer in Docker := "Akihiro KATOU <kaotu.akihiro@gmail.com>"

dockerBaseImage := "dockerfile/java:oracle-java8"
