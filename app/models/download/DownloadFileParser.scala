package models.download

import
  play.api.Logger

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 12/20/12
 * Time: 11:00 AM
 */

object DownloadFileParser {

  private val LinuxFileRegex   = """.*\.tar.gz""".r
  private val MacOSFileRegex   = """.*\.dmg""".r
  private val WindowsFileRegex = """.*\.exe""".r

  def parseLogLine(line: String) {

    val refuseCatcher = """.*""" // Four of the log entries abruptly cut off other entries; this ignores the cut-off junk at the beginning
    val ipRegex       = """(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})"""
    val spacerRegex   = """ - (?:-|ccl)""" // Useless spacer thing
    val dateRegex     = """ \[([^ ]*) -0[56]00\]"""
    val urlRegex      = """ "\w+ /netlogo"""
    val versionRegex  = """/+((?:(?:abmplus|internal)/)?[^/]*)"""
    val filenameRegex = """/([^ ]*?)(?:.backup|\?[^ ]*)?"""
    val httpRegex     = """ HTTP/1.[01]""""
    val statusRegex   = """ 200"""
    val sizeRegex     = """ (\d+)"""

    val regexes = Seq(refuseCatcher, ipRegex, spacerRegex, dateRegex, urlRegex, versionRegex, filenameRegex, httpRegex, statusRegex, sizeRegex)

    val FullRegex = regexes reduceLeft (_ + _) r

    line match {

      case FullRegex(ip, date, version, filename, size) =>

        import org.joda.time.format.DateTimeFormat

        val format             = DateTimeFormat.forPattern("dd/MMM/yyyy:HH:mm:ss")
        val dateTime           = format.parseDateTime(date)
        val (year, month, day) = (dateTime.getYear, dateTime.getMonthOfYear, dateTime.getDayOfMonth)
        val time               = s"${dateTime.getHourOfDay}:${dateTime.getMinuteOfHour}:${dateTime.getSecondOfMinute}"

        import OS._

        val os = filename match {
          case LinuxFileRegex()   => Linux
          case MacOSFileRegex()   => Mac
          case WindowsFileRegex() => Windows
          case _                  => Logger.warn(s"That's odd....  File '$filename' got operating system `Other`."); Other
        }

        val downloadFile = DownloadDBManager.getFileByVersionAndOS(version, os) getOrElse generateDownloadFile(version, os, size.toLong, filename)
        DownloadDBManager.submit(UserDownload(None, ip, downloadFile, year, month, day, time))

      case _ =>
        println(s"Unmatchable download entry: $line")

    }

  }

  private def generateDownloadFile(version: String, os: OS, size: Long, filename: String) : DownloadFile = {

    val initial = DownloadFile(None, version, os, size, filename)
    val idMaybe = DownloadDBManager.submit(initial)

    idMaybe fold ({
      messages =>
        Logger.warn("Failed to write download file to DB or get back ID for it")
        Logger.warn(messages.list.mkString("\n"))
        Logger.warn("Defaulting to `id` being `None`")
        initial
    }, {
      id =>
        initial.copy(id = Option(id))
    })

  }

}
