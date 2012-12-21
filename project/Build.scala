import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "DownloadManager"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "org.scalaz" %% "scalaz-core" % "7.0-SNAPSHOT",
      "mysql" % "mysql-connector-java" % "5.1.18",
      "org.mindrot" % "jbcrypt" % "0.3m",
      "org.scalala" % "scalala_2.9.1" % "1.0.0.RC2"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here
    )

}
