package models.download

import
  anorm._,
    SqlParser._

import
  play.api.db.DB

import
  java.sql.Connection

import
  com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException

import
  scalaz.{ Scalaz, ValidationNEL },
    Scalaz.ToValidationV

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 12/17/12
 * Time: 3:53 PM
 */

import play.api.Play.current

object DownloadDBManager {

  import AnormExtras._

  //@ Maybe add some monadic error-handling here at some point (regarding the size of the `start to end` collection)?
  def getDownloadStatsBetweenDates(start: SimpleDate, end: SimpleDate, osSet: Set[OS] = Set(), versions: Set[String] = Set()) : Seq[(SimpleDate, Long)] =
    start to end map { case date @ SimpleDate(day, month, year) => (date, getDLCountByYMD(year, month, day, osSet, versions)) }

  def getDownloadStatsBetweenMonths(start: SimpleMonth, end: SimpleMonth, osSet: Set[OS] = Set(), versions: Set[String] = Set()) : Seq[(SimpleMonth, Long)] =
    start to end map { case month @ SimpleMonth(m, y) => (month, getDLCountByYM(y, m, osSet, versions)) }

  def getDownloadStatsBetweenYears(start: SimpleYear, end: SimpleYear, osSet: Set[OS] = Set(), versions: Set[String] = Set()) : Seq[(SimpleYear, Long)] =
    start to end map { case year @ SimpleYear(y) => (year, getDLCountByY(y, osSet, versions)) }

  def getDLCountByY(year: Int, osSet: Set[OS] = Set(), versions: Set[String] = Set()) : Long = {
    DB.withConnection { implicit connect =>
      import DBConstants.UserDownloads._
      import DBConstants.{ DownloadFiles => DFConstants }
      val osClause       = generateOSesClause(osSet)
      val versionsClause = generateVersionsClause(versions)
      parseCount(SQL (
       s"""
          |SELECT ${DBConstants.CountKey} FROM $TableName
          |LEFT JOIN ${DFConstants.TableName} ON $TableName.$FileIDKey = ${DFConstants.TableName}.${DFConstants.IDKey}
          |WHERE $YearKey = {year}$osClause$versionsClause;
        """.stripMargin
      ) on (
        "year" -> year
      ))
    }
  }

  def getDLCountByYM(year: Int, month: Int, osSet: Set[OS] = Set(), versions: Set[String] = Set()) : Long = {
    DB.withConnection { implicit connection =>
      import DBConstants.UserDownloads._
      import DBConstants.{ DownloadFiles => DFConstants }
      val osClause       = generateOSesClause(osSet)
      val versionsClause = generateVersionsClause(versions)
      parseCount(SQL (
       s"""
          |SELECT ${DBConstants.CountKey} FROM $TableName
          |LEFT JOIN ${DFConstants.TableName} ON $TableName.$FileIDKey = ${DFConstants.TableName}.${DFConstants.IDKey}
          |WHERE $YearKey = {year} AND $MonthKey = {month}$osClause$versionsClause;
        """.stripMargin
      ) on (
        "year"  -> year,
        "month" -> month
      ))
    }
  }

  def getDLCountByYMD(year: Int, month: Int, day: Int, osSet: Set[OS] = Set(), versions: Set[String] = Set()) : Long = {
    DB.withConnection { implicit connection =>
      import DBConstants.UserDownloads._
      import DBConstants.{ DownloadFiles => DFConstants }
      val osClause       = generateOSesClause(osSet)
      val versionsClause = generateVersionsClause(versions)
      parseCount(SQL (
       s"""
          |SELECT ${DBConstants.CountKey} FROM $TableName
          |LEFT JOIN ${DFConstants.TableName} ON $TableName.$FileIDKey = ${DFConstants.TableName}.${DFConstants.IDKey}
          |WHERE $YearKey = {year} AND $MonthKey = {month} AND $DayKey = {day}$osClause$versionsClause;
        """.stripMargin
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
       s"""
          |SELECT * FROM $TableName
          |WHERE $IDKey = {id};
        """.stripMargin
      ) on (
        "id" -> id
      )).headOption
      opt map (_.successNel[String]) getOrElse (s"No download file found with id $id".failNel)
  }}

  def getFileByVersionAndOS(version: String, os: OS) : ValidationNEL[String, DownloadFile] = {
    DB.withConnection { implicit connection =>
      import DBConstants.DownloadFiles._
      val opt = parseDownloadFiles(SQL (
       s"""
          |SELECT * FROM $TableName
          |WHERE $VersionKey = {version} AND $OSKey = {os};
        """.stripMargin
      ) on (
        "version" -> version,
        "os"      -> os.toString
      )).headOption
      opt map (_.successNel[String]) getOrElse (s"No download file found with version `$version` and OS `$os`".failNel)
    }
  }

  def getAllVersions : Seq[String] = {
    DB.withConnection { implicit connection =>
      import DBConstants.DownloadFiles._
      parseStrings(SQL (
       s"""
          |SELECT DISTINCT $VersionKey FROM $TableName
          |ORDER BY $VersionKey DESC;
        """.stripMargin
      ))(VersionKey)
    }
  }

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
          DownloadFile(Option(id), version, OS.parseOne(os), size, path)
        case _ => raiseDBAccessException
      } *
    }
  }

  private def parseStrings(sql: SimpleSql[Row])(key: String)(implicit connection: Connection) : Seq[String] = {
    val Key = key
    sql as { str(Key) * }
  }

  private def generateOSesClause(osSet: Set[OS]) : String = {
    implicit val f = (os: OS) => os.toString
    generateQueryConstraintClause(osSet, DBConstants.DownloadFiles.OSKey)
  }

  private def generateVersionsClause(versions: Set[String]) : String =
    generateQueryConstraintClause(versions, DBConstants.DownloadFiles.VersionKey)

  private def generateQueryConstraintClause[T <% String](xs: Iterable[T], key: String) : String =
    if (xs.isEmpty)
      ""
    else
      xs map (x => s"""$key = "$x"""") mkString(" AND (", " OR ", ")")

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
       s"""
          |INSERT INTO $TableName
          |($IPKey, $FileIDKey, $YearKey, $MonthKey, $DayKey, $TimeKey) VALUES
          |({ip}, {file}, {year}, {month}, {day}, {time});
        """.stripMargin
      ) on (
        "ip"    -> userDownload.ip,
        "file"  -> userDownload.file.id.get,
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
       s"""
          |INSERT INTO $TableName
          |($VersionKey, $OSKey, $SizeKey, $PathKey) VALUES
          |({version}, {os}, {size}, {path});
        """.stripMargin
      ) on (
        "version" -> downloadFile.version,
        "os"      -> downloadFile.os.toString,
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
       s"""
          |UPDATE $TableName
          |SET $IPKey={ip}, $FileIDKey={file}, $YearKey={year}, $MonthKey={month}, $DayKey={day}, $TimeKey={time}
          |WHERE $IDKey={id};
        """.stripMargin
      ) on (
        "id"    -> userDownload.id.get,
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
       s"""
          |UPDATE $TableName
          |SET $VersionKey={version}, $OSKey={os}, $SizeKey={size}, $PathKey={path}
          |WHERE $IDKey={id};
        """.stripMargin
      ) on (
        "id"      -> downloadFile.id.get,
        "version" -> downloadFile.version,
        "os"      -> downloadFile.os.toString,
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
      case ex: MySQLIntegrityConstraintViolationException => s"SQL constraint violated: ${ex.getMessage}".failNel
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

