package controllers

import
  java.io.File

import
  play.api.mvc.{ Action, Controller }

import
  models.download.DownloadFileParser

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 1/31/13
 * Time: 2:34 PM
 */

object Script extends Controller {

  def parseLogs = Action {
//    val file = new File("/home/jason/Desktop/NetLogoLogs/pruned/")
//    file.listFiles().toSeq.par foreach (file => io.Source.fromFile(file).getLines() foreach DownloadFileParser.parseLogLine)
    Ok
  }

}
