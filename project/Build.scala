import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "DownloadManager"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "org.scalaz" %% "scalaz-core" % "7.0-SNAPSHOT",
      "mysql" % "mysql-connector-java" % "5.1.18"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here
    )

}
