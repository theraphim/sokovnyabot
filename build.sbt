name := "bototest"

version := "0.1"

scalaVersion := "2.12.11"

enablePlugins(JavaAppPackaging)

val bot4s = "4.4.0-RC2"

libraryDependencies ++= Seq(
  "com.bot4s" %% "telegram-core" % bot4s,
  "com.bot4s" %% "telegram-akka" % bot4s,
  "com.github.pureconfig" %% "pureconfig" % "0.12.3"
)