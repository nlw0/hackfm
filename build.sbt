name := "scala-hackfm"

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= List(
  "com.typesafe.akka" %% "akka-actor" % "2.3.12",
  "com.typesafe.akka" %% "akka-http-experimental" % "1.0",
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
  "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2"
)
