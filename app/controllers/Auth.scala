package controllers

import play.api.{ data, mvc }
import mvc.{ Action, Controller, Security }
import data.{ Form, Forms }, Forms.{ text, tuple }

import models.auth.AuthDBManager

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
      (_ match { case (username, pw) => AuthDBManager.validates(username, pw) })
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

