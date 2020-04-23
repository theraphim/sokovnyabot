name := "sokovnyabot"

version := "0.1"

scalaVersion := "2.12.11"

enablePlugins(JavaAppPackaging)

scalacOptions ++= Seq(
  "-Xfatal-warnings",  // New lines for each options
  "-deprecation",
  "-unchecked",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps",
  "-opt:l:method",
  "-opt:l:inline",
  "-opt-inline-from:<sources>"
)

val bot4s = "4.4.0-RC2"

libraryDependencies ++= Seq(
  "com.bot4s" %% "telegram-core" % bot4s,
  "com.bot4s" %% "telegram-akka" % bot4s,
  "com.github.pureconfig" %% "pureconfig" % "0.12.3"
)
