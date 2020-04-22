name := "bototest"

version := "0.1"

scalaVersion := "2.13.1"

enablePlugins(JavaAppPackaging)

libraryDependencies ++= (
  "com.bot4s" %% "telegram-core" % "4.4.0-RC2"
)
