package controllers

import
  play.api.{ data, mvc },
    data.{ Form, Forms },
      Forms.{ text, tuple },
    mvc.{ Action, Controller, Security }

import
  models.auth.AuthDBManager

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 12/19/12
 * Time: 2:05 PM
 */

object Auth extends Controller {

  val loginForm = Form (
    tuple (
      "username" -> text,
      "password" -> text
    ) verifying (
      "Invalid username or password",
      (AuthDBManager.validates _).tupled
    )
  )

  def login = Action {
    implicit request =>
      Ok(views.html.login(loginForm))
  }

  def authenticate = Action {
    implicit request =>
      loginForm.bindFromRequest fold (
        form => BadRequest(views.html.login(form)),
        user => Redirect(routes.Application.downloads).withSession(Security.username -> user._1)
      )
  }

  def logout = Action {
    Redirect(routes.Auth.login).withNewSession.flashing("success" -> "You are now logged out.")
  }

}

