package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def downloads = Action {
    Ok(views.html.downloads("Well done, good sir"))
  }

  def testbed = Action {
    Ok(views.html.testbed("WHAT DO YOU EVEN THINK YOU'RE TESTING?!"))
  }
  
}
