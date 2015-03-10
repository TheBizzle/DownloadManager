package controllers

import
  play.api.mvc.{ Action, AnyContent, EssentialAction, Request, RequestHeader, Result, Results, Security }

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 12/19/12
 * Time: 2:25 PM
 */

trait Secured {

  def username(request: RequestHeader): Option[String] =
    request.session.get(Security.username)

  def onUnauthorized(request: RequestHeader): Result =
    Results.Redirect(routes.Auth.login)

  def withAuth(f: => String => Request[AnyContent] => Result): EssentialAction =
    Security.Authenticated(username, onUnauthorized)(user => Action(request => f(user)(request)))

}

