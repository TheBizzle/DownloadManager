lazy val root = (project in file(".")).enablePlugins(PlayScala)

name := "DownloadManager"

version := "1.1"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

scalaVersion := "2.11.6"

scalacOptions += "-language:_"

libraryDependencies ++= Seq(
  anorm,
  jdbc,
  "org.scalaz" %% "scalaz-core" % "7.1.1",
  "mysql" % "mysql-connector-java" % "5.1.34",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "com.googlecode.charts4j" % "charts4j" % "1.4-SNAPSHOT" from "http://ccl.northwestern.edu/devel/charts4j-1.4-SNAPSHOT.jar"
)
