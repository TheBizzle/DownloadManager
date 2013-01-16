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


  val dataForm = Form (
    tuple (
      "os"         -> text,
      "start_day"  -> text,
      "end_day"    -> text,
      "quantum"    -> text,
      "graph_type" -> text
    ) verifying (
      "Invalid username or password",
      (_ match { case (os, start, end, quantum, graphType) => validateData(os, start, end, quantum, graphType) })
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

          val rawData     = quantumFunc(startDate, endDate, osSet) map { case (q, c) => (q.asDateString, c) }
          val refinedData = prepareData(rawData, graphType)

          Grapher.fromStrCountPairs(refinedData)

        }
      )
  }

  private def prepareData(data: Seq[(String, Long)], gt: GraphType) : (Seq[(String, Long)]) = {
    gt match {
      case Discrete   => data
      case Cumulative => data.scanLeft(("", 0L)){ case ((ac, ax), (bc, bx)) => (bc, (ax + bx)) }.drop(1)
    }
  }

  private def determineQuantumFunction(quantumStr: String) : (SimpleDate, SimpleDate, Set[OS]) => Seq[(Quantum[_], Long)]  = {
    quantumStr.toLowerCase match {
      case "day"   => DownloadDBManager.getDownloadStatsBetweenDates _
      case "month" => { case (s, e, os) => DownloadDBManager.getDownloadStatsBetweenMonths(s.toSimpleMonth, e.toSimpleMonth, os) }
      case "year"  => { case (s, e, os) => DownloadDBManager.getDownloadStatsBetweenYears(s.toSimpleMonth.toSimpleYear, e.toSimpleMonth.toSimpleYear, os) }
      case _       => throw new Exception("Boom!") //@
    }
  }

  private def validateData(os: String, start: String, end: String, quantum: String, graphType: String) = true //@

}
