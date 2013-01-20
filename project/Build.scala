import
  sbt._,
    Keys._,
    PlayProject._

object ApplicationBuild extends Build {

    val appName         = "DownloadManager"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "org.scalaz" %% "scalaz-core" % "7.0-SNAPSHOT",
      "mysql" % "mysql-connector-java" % "5.1.18",
      "org.mindrot" % "jbcrypt" % "0.3m",
      "com.googlecode.charts4j" % "charts4j" % "1.4-SNAPSHOT" from
        "http://ccl.northwestern.edu/devel/charts4j-1.4-SNAPSHOT.jar"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here
    )

}
