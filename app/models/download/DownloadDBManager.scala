package models.download

import anorm._
import anorm.SqlParser._
import play.api.db.DB

import java.sql.Connection

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException

import scalaz.{ Scalaz, ValidationNEL }, Scalaz.ToValidationV

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 12/17/12
 * Time: 3:53 PM
 */

import play.api.Play.current

object DownloadDBManager {

  import AnormExtras._

  def getDownloadStatsBetween(startDate: SimpleDate, endDate: SimpleDate) : Seq[(SimpleDate, Long)] =
    startDate to endDate map { case date @ SimpleDate(day, month, year) => (date, getDLCountByYMD(day, month, year)) }

  def getDLCountByY(year: Int) : Long = {
    DB.withConnection { implicit connect =>
      import DBConstants.UserDownloads._
      parseCount(SQL (
        """
          |SELECT %s FROM %s
          |WHERE %s = {year};
        """.stripMargin.format(DBConstants.CountKey, TableName, YearKey) //@ Do want string interpolation!
      ) on (
        "year" -> year
      ))
    }
  }

  def getDLCountByYM(year: Int, month: Int) : Long = {
    DB.withConnection { implicit connection =>
      import DBConstants.UserDownloads._
      parseCount(SQL (
        """
          |SELECT %s FROM %s
          |WHERE %s = {year} AND %s = {month};
        """.stripMargin.format(DBConstants.CountKey, TableName, YearKey, MonthKey)
      ) on (
        "year"  -> year,
        "month" -> month
      ))
    }
  }

  def getDLCountByYMD(year: Int, month: Int, day: Int) : Long = {
    DB.withConnection { implicit connection =>
      import DBConstants.UserDownloads._
      parseCount(SQL (
        """
          |SELECT %s FROM %s
          |WHERE %s = {year} AND %s = {month} AND %s = {day};
        """.stripMargin.format(DBConstants.CountKey, TableName, YearKey, MonthKey, DayKey)
      ) on (
        "year"  -> year,
        "month" -> month,
        "day"   -> day
      ))
    }
  }

  def getFileByID(id: Long) : ValidationNEL[String, DownloadFile] = {
    DB.withConnection { implicit connection =>
    import DBConstants.DownloadFiles._
    val opt = parseDownloadFiles(SQL (
      """
        |SELECT * FROM %s
        |WHERE %s = {id};
      """.stripMargin.format(TableName, IDKey)
    ) on (
      "id" -> id
    )).headOption
    opt map (_.successNel[String]) getOrElse ("No download file found with id %s".format(id).failNel)
  }}

  private def parseCount(sql: SimpleSql[Row])(implicit connection: Connection) : Long =
    sql as { long(DBConstants.CountKey).single }

  private def parseUserDownloads(sql: SimpleSql[Row])(implicit connection: Connection) : Seq[UserDownload] = {
    import DBConstants.UserDownloads._
    sql as {
      long(IDKey) ~ str(IPKey) ~ long(FileIDKey) ~ int(YearKey) ~ int(MonthKey) ~ int(DayKey) ~ str(TimeKey) map {
        case id ~ ip ~ file_id ~ year ~ month ~ day ~ time =>
          getFileByID(file_id) fold (
            (nel  => throw new MySQLIntegrityConstraintViolationException(nel.list.mkString("\n"))),
            (file => UserDownload(Option(id), ip, file, year, month, day, time))
          )
        case _ => raiseDBAccessException
      } *
    }
  }

  private def parseDownloadFiles(sql: SimpleSql[Row])(implicit connection: Connection) : Seq[DownloadFile] = {
    import DBConstants.DownloadFiles._
    sql as {
      long(IDKey) ~ str(VersionKey) ~ str(OSKey) ~ long(SizeKey) ~ str(PathKey) map {
        case id ~ version ~ os ~ size ~ path =>
          DownloadFile(Option(id), version, OS(os), size, path)
        case _ => raiseDBAccessException
      } *
    }
  }

  def submit[T <% Submittable](submission: T) : ValidationNEL[String, Long] = submission.submit
  def update[T <% Updatable]  (update: T)                                   { update.update() }

}

sealed trait Submittable {
  def submit : ValidationNEL[String, Long]
}

private object Submittable {

  import AnormExtras.tryInsert

  implicit def userDownload2Submittable(userDownload: UserDownload) = new Submittable {
    override def submit : ValidationNEL[String, Long] = DB.withConnection { implicit connection =>
      import DBConstants.UserDownloads._
      val sql = SQL (
        """
          |INSERT INTO %s
          |(%s, %s, %s, %s, %s, %s) VALUES
          |({ip}, {file_ID}, {year}, {month}, {day}, {time});
        """.stripMargin.format(TableName, IPKey, FileIDKey, YearKey, MonthKey, DayKey, TimeKey)
      ) on (
        "ip"    -> userDownload.ip,
        "file"  -> userDownload.file,
        "year"  -> userDownload.year,
        "month" -> userDownload.month,
        "day"   -> userDownload.day,
        "time"  -> userDownload.time
      )
      tryInsert(sql)(_.get.successNel[String])
    }
  }

  implicit def downloadFile2Submittable(downloadFile: DownloadFile) = new Submittable {
    override def submit : ValidationNEL[String, Long] = DB.withConnection { implicit connection =>
      import DBConstants.DownloadFiles._
      val sql = SQL (
        """
          |INSERT INTO %s
          |(%s, %s, %s, %s) VALUES
          |({version}, {os}, {size}, {path});
        """.stripMargin.format(TableName, VersionKey, OSKey, SizeKey, PathKey)
      ) on (
        "version" -> downloadFile.version,
        "os"      -> downloadFile.os,
        "size"    -> downloadFile.size,
        "path"    -> downloadFile.path
      )
      tryInsert(sql)(_.get.successNel[String])
    }
  }

}

sealed trait Updatable {
  def update()
}

private object Updatable {

  implicit def userDownload2Updatable(userDownload: UserDownload) = new Updatable {
    override def update() { DB.withConnection { implicit connection =>
      import DBConstants.UserDownloads._
      val sql = SQL (
        """
          |UPDATE %s
          |SET %s={ip}, %s={file}, %s={year}, %s={month}, %s={day}, %s={time}
          |WHERE %s={id};
        """.stripMargin.format(TableName, IPKey, FileIDKey, YearKey, MonthKey, DayKey, TimeKey, IDKey)
      ) on (
        "id"    -> userDownload.id,
        "ip"    -> userDownload.ip,
        "file"  -> userDownload.file,
        "year"  -> userDownload.year,
        "month" -> userDownload.month,
        "day"   -> userDownload.day,
        "time"  -> userDownload.time
      )
      sql.executeUpdate()
    }}
  }

  implicit def downloadFile2Updatable(downloadFile: DownloadFile) = new Updatable {
    override def update() { DB.withConnection { implicit connection =>
      import DBConstants.DownloadFiles._
      val sql = SQL (
        """
          |UPDATE %s
          |SET %s={version}, %s={os}, %s={size}, %s={path}
          |WHERE %s={id};
        """.stripMargin.format(TableName, VersionKey, OSKey, SizeKey, PathKey, IDKey)
      ) on (
        "id"      -> downloadFile.id,
        "version" -> downloadFile.version,
        "os"      -> downloadFile.os,
        "size"    -> downloadFile.size,
        "path"    -> downloadFile.path
      )
      sql.executeUpdate()
    }}
  }

}

object AnormExtras {
  import java.math.{ BigInteger => JBigInt }
  def timestamp(columnName: String) : RowParser[Long] = get[JBigInt](columnName)(implicitly[Column[JBigInt]]) map (new BigInt(_).toLong)
  def raiseDBAccessException = throw new java.sql.SQLException("Retrieved data from database in unexpected format.")
  def tryInsert(sql: SimpleSql[Row])(f: (Option[Long]) => ValidationNEL[String, Long])
               (implicit connection: java.sql.Connection) : ValidationNEL[String, Long] = {
    try sql.executeInsert() match { case x => f(x) }
    catch {
      case ex: MySQLIntegrityConstraintViolationException => "SQL constraint violated: %s".format(ex.getMessage).failNel
    }
  }
}

private object DBConstants {

  trait Table {
    def TableName: String
  }

  val CountKey = "count(*)"

  object UserDownloads extends Table {

    override val TableName = "user_downloads"

    val DayKey    = "day"
    val FileIDKey = "file_id"
    val IDKey     = "id"
    val IPKey     = "ip"
    val MonthKey  = "month"
    val TimeKey   = "time"
    val YearKey   = "year"

  }

  object DownloadFiles extends Table {

    override val TableName = "download_files"

    val IDKey      = "id"
    val OSKey      = "os"
    val PathKey    = "path"
    val SizeKey    = "size"
    val VersionKey = "version"

  }

}

