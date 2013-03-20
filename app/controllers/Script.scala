package controllers

import
  scala.concurrent.duration._

import
  java.io.{ File, FilenameFilter }

import
  org.joda.time.{ DateTime, Interval }

import
  play.{ api, libs },
    api.{ Logger, mvc, Play },
      mvc.{ Action, Controller },
    libs.Akka

import
  models.download.{ DownloadDBManager, DownloadFileParser }

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 1/31/13
 * Time: 2:34 PM
 */

object Script extends Controller {

  // Check for new downloads every day at midnight
  Akka.system.scheduler.schedule(timeTillMidnight, 1.days) {

    val startFunc = (start: DateTime)                   => s"Doing my daily chores for ${start.toLocalDate.toString}..."
    val endFunc   = (end: DateTime, interval: Interval) => s"Chores completed!"

    logAndTimeActivity(startFunc, endFunc) {
      submitNewDownloads()
    }

  }

  def init = Action {
    Ok
  }

  def parseLogs = Action {

    if (getSettingAsBoolean("script.activated")) {
      val fileOpt = getSettingOpt("script.logs.dir") map (new File(_))
      fileOpt map {
        _.listFiles().toSeq.par foreach (file => io.Source.fromFile(file).getLines() foreach (DownloadFileParser.parseLogLine(_) foreach (DownloadDBManager.submit(_))))
      } getOrElse {
        Logger.warn("No `script.logs_dir` given in the application's configuration")
      }
    }
    else
      Logger.warn("Someone's up to no good...")

    Ok

  }

  def parseNewLogs = Action {

    if (getSettingAsBoolean("script.activated"))
      submitNewDownloads()
    else
      Logger.warn("Something fishy's going on...")

    Ok

  }

  private def submitNewDownloads() {

    val startFunc = (start: DateTime)                   => s"Starting new log import (${start.getHourOfDay}:${start.getMinuteOfHour})"
    val endFunc   = (end: DateTime, interval: Interval) => {
      val time = interval.toDuration.getStandardSeconds
      s"Log import complete!  Import took $time seconds!  (AKA ${time.toDouble / 60} minutes)"
    }

    logAndTimeActivity(startFunc, endFunc) {

      val fileOpt           = getSettingOpt("script.logs.dir") map (new File(_))
      val shouldParallelize = getSettingAsBoolean("script.logs.read.parallel")

      val rawFiles = listFilesEndingWith("access_log", fileOpt) ++ listFilesEndingWith("access_log.1", fileOpt)
      val rawFunc  = (file: File) => io.Source.fromFile(file).getLines()

      val zipFiles = listFilesEndingWith("access_log.1.gz", fileOpt)
      val zipFunc  = (file: File) => {

        import scala.io.BufferedSource
        import java.io.FileInputStream
        import java.util.zip.GZIPInputStream

        val fileStream = new FileInputStream(file)
        val gzipStream = new GZIPInputStream(fileStream)
        val source     = new BufferedSource(gzipStream)

        source.getLines()

      }

      val fileFuncPairs = Seq((rawFiles, rawFunc), (zipFiles, zipFunc))

      fileFuncPairs map {
        case (files, func) => (if (shouldParallelize) files.par else files, func)
      } map {
        case (files, func) => files map func foreach {
          lines =>
            lines foreach {
              line =>
                DownloadFileParser.parseLogLineIfRelevant(line) foreach {
                  case download if (!DownloadDBManager.existsDownload(download)) =>
                    DownloadDBManager.submit(download)
                  case _ =>
                  // Do nothing
                }
            }
        }
      }

    }

  }

  private def listFilesEndingWith(endsWithStr: String, fileOpt: Option[File]) : Seq[File] =
    fileOpt map {
      file =>
        val filter = new FilenameFilter {
          override def accept(parent: File, filename: String) = filename.endsWith(endsWithStr)
        }
        file.listFiles(filter).toSeq
    } getOrElse (Seq())

  private def getSettingOpt(key: String) = Play.application.configuration.getString(key)

  private def getSettingAsBoolean(key: String) = getSettingOpt(key) map (_ == "true") getOrElse false

  private def timeTillMidnight : FiniteDuration = {

    val now      = new DateTime
    val midnight = now.toLocalDate.plusDays(1).toDateTimeAtStartOfDay(now.getZone)

    val millisToMidnight = new Interval(now, midnight).toDurationMillis

    new FiniteDuration(millisToMidnight, MILLISECONDS)


  }

  private def logAndTimeActivity[T](startMsgFunc: (DateTime) => String, endMsgFunc: (DateTime, Interval) => String)(activity: => T) : T = {

    val start = new DateTime
    Logger.info(startMsgFunc(start))

    val ret  = activity

    val end  = new DateTime
    val time = new Interval(start, end)
    Logger.info(endMsgFunc(end, time))

    ret

  }

}
