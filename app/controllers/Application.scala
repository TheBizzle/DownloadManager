package controllers

import
  scalaz.ValidationNel

import
  play.api.{ data, Logger, mvc },
    data.{ Form, Forms },
      Forms.{ text, tuple },
    mvc.{ Action, Controller }

import
  models.download.{ DownloadDBManager, GraphType, Quantum, OS, SimpleDate }

import
  GraphType.{ Cumulative, Discrete }

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

          val osSet            = OS.parseMany(criteria._1)
          val startDate        = SimpleDate(criteria._2)
          val endDate          = SimpleDate(criteria._3)
          val quantumMaybeFunc = determineQuantumFunction(criteria._4)
          val graphType        = GraphType(criteria._5)
          val versions         = parseVersions(criteria._6)

          val rawDataMaybe     = quantumMaybeFunc(startDate, endDate, osSet, versions) map (_ map { case (q, c) => (q.asDateString, c) })
          val refinedDataMaybe = rawDataMaybe map (prepareData(_, graphType))

          Grapher.fromStrCountPairsMaybe(refinedDataMaybe)

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

  private def determineQuantumFunction(quantumStr: String) : (SimpleDate, SimpleDate, Set[OS], Set[String]) => ValidationNel[String, Seq[(Quantum[_], Long)]] = {
    quantumStr.toLowerCase match {
      case "day" =>
        DownloadDBManager.getDownloadStatsBetweenDates _
      case "month" =>
        { case (s, e, os, versions) => DownloadDBManager.getDownloadStatsBetweenMonths(s.toSimpleMonth, e.toSimpleMonth, os, versions) }
      case "year" =>
        { case (s, e, os, versions) => DownloadDBManager.getDownloadStatsBetweenYears(s.toSimpleMonth.toSimpleYear, e.toSimpleMonth.toSimpleYear, os, versions) }
      case x => {
        case (s, e, os, versions) =>
          Logger.warn(s"Unknown quantum ($x) requested; giving `month`")
          DownloadDBManager.getDownloadStatsBetweenMonths(s.toSimpleMonth, e.toSimpleMonth, os, versions)
      }
    }
  }

  private def validateData(os: String, start: String, end: String, quantum: String, graphType: String, versions: String) = true //@

}
