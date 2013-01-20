package models.download

import
  java.io.File

import
  scalaz.{ Scalaz, ValidationNEL },
    Scalaz.ToValidationV

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 12/20/12
 * Time: 11:00 AM
 */

object DownloadFileParser {

  private val RelevancyRegex   = """.*/netlogo/((?:3DPreview|\d+\.)[^/]+)/(.*)""".r
  private val LinuxFileRegex   = """.*\.tar.gz""".r
  private val MacOSFileRegex   = """.*\.dmg""".r
  private val WindowsFileRegex = """.*\.exe""".r

  def fromPath(url: String) : ValidationNEL[String, DownloadFile] =
    url match {
      case RelevancyRegex(version, remainder) => parsePath(version, remainder)
      case _                                  => "Invalid path supplied".failNel
    }

  private def parsePath(version: String, path: String) : ValidationNEL[String, DownloadFile] = {

    import OS._

    // Note to self: All non-VM Windows installers match this regex: .*NoVM.*\.exe
    val osMaybe = path match {
      case LinuxFileRegex()   => Linux.successNel[String]
      case MacOSFileRegex()   => Mac.successNel[String]
      case WindowsFileRegex() => Windows.successNel[String]
      case _                  => "Non-download file".failNel
    }

    osMaybe flatMap {
      os =>
        val relativePath = "%s%s%s".format(version, File.separator, path)
        val size = new File(play.Configuration.root.getString("downloads.base_path") + File.separator + relativePath).length
        DownloadFile(None, version, os, size, relativePath).successNel[String]
    }

  }

}
