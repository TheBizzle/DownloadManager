package models.auth

import
  anorm._,
    SqlParser._,
  play.api.db.DB

import
  org.mindrot.jbcrypt.BCrypt

import
  scalaz.{ Scalaz, ValidationNel },
    Scalaz.ToValidationV

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 12/19/12
 * Time: 2:14 PM
 */

import play.api.Play.current

object AuthDBManager {

  def validates(username: String, password: String) =
    pwHashForUsername(username) map (hash => BCrypt.checkpw(password, hash)) fold((_ => false), identity)

  private def pwHashForUsername(username: String) : ValidationNel[String, String] = {
    DB.withConnection { implicit connection =>
      import DBConstants.Users._
      SQL (
       s"""
          |SELECT $PWKey FROM $TableName
          |WHERE $NameKey = {name};
        """.stripMargin
      ) on (
        "name" -> username
      ) as {
        str(PWKey).singleOpt
      } map (_.successNel[String]) getOrElse (s"No entry for user with `name` == $username".failNel)
    }
  }

}

private object DBConstants {

  trait Table {
    def TableName: String
  }

  object Users extends Table {

    override lazy val TableName = "users"

    val NameKey = "name"
    val PWKey   = "pw"

  }

}
