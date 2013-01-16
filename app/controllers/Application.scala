package controllers

import play.api.{ data, mvc }
import mvc.{ Action, Controller }
import data.{ Form, Forms }, Forms.{ text, tuple }

import models.download.{ DownloadDBManager, Quantum, OS, SimpleDate }

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
      "os"        -> text,
      "start_day" -> text,
      "end_day"   -> text,
      "quantum"   -> text
    ) verifying (
      "Invalid username or password",
      (_ match { case (os, start, end, quantum) => validateData(os, start, end, quantum) })
    )
  )

  def requestData = withAuth {
    username => implicit request =>
      dataForm.bindFromRequest fold (
        form     => BadRequest("Crap"),
        criteria => Ok {
          val osSet     = OS.parseMany(criteria._1)
          val startDate = SimpleDate(criteria._2)
          val endDate   = SimpleDate(criteria._3)
          val f         = determineQuantumFunction(criteria._4)
          Grapher.fromStrCountPairs(f(startDate, endDate, osSet) map { case (q, c) => (q.asDateString, c) })
        }
      )
  }

  private def determineQuantumFunction(quantumStr: String) : (SimpleDate, SimpleDate, Set[OS]) => Seq[(Quantum[_], Long)]  = {
    quantumStr match {
      case "Day"   => DownloadDBManager.getDownloadStatsBetweenDates _
      case "Month" => { case (s, e, os) => DownloadDBManager.getDownloadStatsBetweenMonths(s.toSimpleMonth, e.toSimpleMonth, os) }
      case "Year"  => { case (s, e, os) => DownloadDBManager.getDownloadStatsBetweenYears(s.toSimpleMonth.toSimpleYear, e.toSimpleMonth.toSimpleYear, os) }
      case _       => throw new Exception("Boom!") //@
    }
  }

  private def validateData(os: String, start: String, end: String, quantum: String) = true //@

}
