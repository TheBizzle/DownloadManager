package controllers

import play.api.{ data, mvc }
import mvc.{ Action, Controller }
import data.{ Form, Forms }, Forms.{ text, tuple }

import models.download.{ DownloadDBManager, GraphType, Quantum, OS, SimpleDate }
import GraphType.{ Cumulative, Discrete }

object Application extends Controller with Secured {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def downloads = withAuth {
    username => implicit request =>
      Ok(views.html.downloads(username))
  }

  def versions = withAuth {
    username => implicit request =>
      Ok(DownloadDBManager.getAllVersions.mkString("[\"", "\", \"", "\"]"))
  }

  val dataForm = Form (
    tuple (
      "os"         -> text,
      "start_day"  -> text,
      "end_day"    -> text,
      "quantum"    -> text,
      "graph_type" -> text,
      "versions"   -> text
    ) verifying (
      "Invalid query data",
      (_ match { case (os, start, end, quantum, graphType, versions) => validateData(os, start, end, quantum, graphType, versions) })
    )
  )

  def requestData = withAuth {
    username => implicit request =>
      dataForm.bindFromRequest fold (
        form     => BadRequest("Crap"),
        criteria => Ok {

          val osSet       = OS.parseMany(criteria._1)
          val startDate   = SimpleDate(criteria._2)
          val endDate     = SimpleDate(criteria._3)
          val quantumFunc = determineQuantumFunction(criteria._4)
          val graphType   = GraphType(criteria._5)
          val versions    = parseVersions(criteria._6)

          val rawData     = quantumFunc(startDate, endDate, osSet, versions) map { case (q, c) => (q.asDateString, c) }
          val refinedData = prepareData(rawData, graphType)

          Grapher.fromStrCountPairs(refinedData)

        }
      )
  }

  private def parseVersions(s: String) : Set[String] =
    if (s.toLowerCase == "all")
      Set()
    else
      s split '|' toSet

  private def prepareData(data: Seq[(String, Long)], gt: GraphType) : (Seq[(String, Long)]) = {
    gt match {
      case Discrete   => data
      case Cumulative => data.scanLeft(("", 0L)){ case ((ac, ax), (bc, bx)) => (bc, (ax + bx)) }.drop(1)
    }
  }

  private def determineQuantumFunction(quantumStr: String) : (SimpleDate, SimpleDate, Set[OS], Set[String]) => Seq[(Quantum[_], Long)]  = {
    quantumStr.toLowerCase match {
      case "day" =>
        DownloadDBManager.getDownloadStatsBetweenDates _
      case "month" =>
        { case (s, e, os, versions) => DownloadDBManager.getDownloadStatsBetweenMonths(s.toSimpleMonth, e.toSimpleMonth, os, versions) }
      case "year" =>
        { case (s, e, os, versions) => DownloadDBManager.getDownloadStatsBetweenYears(s.toSimpleMonth.toSimpleYear, e.toSimpleMonth.toSimpleYear, os, versions) }
      case _ =>
        throw new Exception("Boom!") //@
    }
  }

  private def validateData(os: String, start: String, end: String, quantum: String, graphType: String, versions: String) = true //@

}
