name := "cluster-managment"

version := "1.0"

scalaVersion := "2.12.1"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.9",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.9",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.9" % Test,
  "joda-time" % "joda-time" % "2.9.9",
  "ai.api" % "libai" % "1.4.8",
  "org.apache.mesos" % "mesos" % "1.4.1"
)

