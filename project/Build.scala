import
  sbt._,
    Keys._

import
  play.Project._

object ApplicationBuild extends Build {

    val appName         = "DownloadManager"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      anorm,
      jdbc,
      "org.scalaz" %% "scalaz-core" % "7.0-SNAPSHOT",
      "mysql" % "mysql-connector-java" % "5.1.18",
      "org.mindrot" % "jbcrypt" % "0.3m",
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
      scalacOptions += "-language:_"
    )

}
