package controllers

import play.api._
import play.api.mvc._

import models.download.DownloadDBManager

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def downloads = Action {
    Ok(views.html.downloads("Well done, good sir"))
  }

}
