package controllers

import play.api.mvc.{ Action, Controller }

import models.download.DownloadDBManager

object Application extends Controller with Secured {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def downloads = withAuth {
    username => implicit request =>
      Ok(views.html.downloads(username))
  }

}
