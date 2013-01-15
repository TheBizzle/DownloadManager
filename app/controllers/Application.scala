package controllers

import play.api.{ data, mvc }
import mvc.{ Action, Controller }
import data.{ Form, Forms }, Forms.{ text, tuple }

import models.download.{ DownloadDBManager, SimpleDate }

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
      "end_day"   -> text
    ) verifying (
      "Invalid username or password",
      (_ match { case (os, start, end) => validateData(os, start, end) })
    )
  )

  def requestData = withAuth {
    username => implicit request =>
      dataForm.bindFromRequest fold (
        form     => BadRequest("Crap"),
        criteria => Ok {
          def parseOS(str: String) = ""
          val os        = parseOS(criteria._1)
          val startDate = SimpleDate(criteria._2)
          val endDate   = SimpleDate(criteria._3)
          Grapher.fromStrCountPairs(DownloadDBManager.getDownloadStatsBetween(startDate, endDate) map { case (q, c) => (q.asDateString, c) })
        }
      )
  }

  private def validateData(os: String, start: String, end: String) = true //@

}
