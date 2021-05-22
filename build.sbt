name := "sokovnyabot"

version := "0.3"

maintainer := "i@stingr.net"

scalaVersion := "2.13.6"

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

updateOptions := updateOptions.value.withCachedResolution(true)

resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.jcenterRepo,
  Resolver.sonatypeRepo("snapshots"),
  Resolver.typesafeRepo("releases")
)

val bot4s = "5.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.bot4s" %% "telegram-core" % bot4s,
  "com.bot4s" %% "telegram-akka" % bot4s,
  "biz.enef" %% "slogging" % "0.6.2",
  "com.github.pureconfig" %% "pureconfig" % "0.15.0"
)
