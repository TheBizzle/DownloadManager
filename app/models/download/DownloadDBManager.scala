package models.download

import
  anorm._,
    SqlParser._

import
  play.api.db.DB

import
  java.sql.{ Connection, SQLException }

import
  com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException

import
  scalaz.{ Scalaz, ValidationNel },
    Scalaz.ToValidationOps

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 12/17/12
 * Time: 3:53 PM
 */

import play.api.Play.current

object DownloadDBManager {

  import AnormExtras._

  private type VNel[T] = ValidationNel[String, T]

  def getDownloadStatsBetweenDates(start: SimpleDate, end: SimpleDate, osSet: Set[OS] = Set(), versions: Set[String] = Set()): VNel[Seq[(SimpleDate, Long)]] =
    generateDateRangeMaybe(start, end) map (_ map { case date @ SimpleDate(d, m, y) => (date, getDLCountByYMD(y, m, d, osSet, versions)) })

  def getDownloadStatsBetweenMonths(start: SimpleMonth, end: SimpleMonth, osSet: Set[OS] = Set(), versions: Set[String] = Set()): VNel[Seq[(SimpleMonth, Long)]] =
    generateDateRangeMaybe(start, end) map (_ map { case month @ SimpleMonth(m, y) => (month, getDLCountByYM(y, m, osSet, versions)) })

  def getDownloadStatsBetweenYears(start: SimpleYear, end: SimpleYear, osSet: Set[OS] = Set(), versions: Set[String] = Set()): VNel[Seq[(SimpleYear, Long)]] =
    generateDateRangeMaybe(start, end) map (_ map { case year @ SimpleYear(y) => (year, getDLCountByY(y, osSet, versions)) })

  def getDLCountByY(year: Int, osSet: Set[OS] = Set(), versions: Set[String] = Set()): Long = {
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

  def getDLCountByYM(year: Int, month: Int, osSet: Set[OS] = Set(), versions: Set[String] = Set()): Long = {
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

  def getDLCountByYMD(year: Int, month: Int, day: Int, osSet: Set[OS] = Set(), versions: Set[String] = Set()): Long = {
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

  def getFileByID(id: Long): ValidationNel[String, DownloadFile] = {
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
      opt.fold(s"No download file found with id $id".failureNel[DownloadFile])(_.successNel[String])
  }}

  def getFileByVersionAndOS(version: String, os: OS): ValidationNel[String, DownloadFile] = {
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
      opt.fold(s"No download file found with version `$version` and OS `$os`".failureNel[DownloadFile])(_.successNel[String])
    }
  }

  def getAllVersions: Seq[String] = {
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

  def existsDownload(download: UserDownload): Boolean = {
    DB.withConnection { implicit connection =>
      import DBConstants.UserDownloads._
      import download._
      parseCount(SQL (
        s"""
          |SELECT ${DBConstants.CountKey} FROM $TableName
          |WHERE $YearKey = {year} AND $MonthKey = {month} AND $DayKey = {day}
          |AND $TimeKey = {time} AND $IPKey = {ip} AND $FileIDKey = {file_id};
        """.stripMargin
      ) on (
        "year"    -> year,
        "month"   -> month,
        "day"     -> day,
        "time"    -> time,
        "ip"      -> ip,
        "file_id" -> file.id.get
      )) > 0
    }
  }

  private def parseCount(sql: SimpleSql[Row])(implicit connection: Connection): Long =
    sql as { long(DBConstants.CountKey).single }

  private def parseUserDownloads(sql: SimpleSql[Row])(implicit connection: Connection): Seq[UserDownload] = {
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

  private def parseDownloadFiles(sql: SimpleSql[Row])(implicit connection: Connection): Seq[DownloadFile] = {
    import DBConstants.DownloadFiles._
    sql as {
      long(IDKey) ~ str(VersionKey) ~ str(OSKey) ~ long(SizeKey) ~ str(PathKey) map {
        case id ~ version ~ os ~ size ~ path =>
          DownloadFile(Option(id), version, OS.parseOne(os), size, path)
        case _ => raiseDBAccessException
      } *
    }
  }

  private def parseStrings(sql: SimpleSql[Row])(key: String)(implicit connection: Connection): Seq[String] = {
    val Key = key
    sql as { str(Key) * }
  }

  private def generateOSesClause(osSet: Set[OS]): String = {
    implicit val f = (os: OS) => os.toString
    generateQueryConstraintClause(osSet, DBConstants.DownloadFiles.OSKey)
  }

  private def generateVersionsClause(versions: Set[String]): String =
    generateQueryConstraintClause(versions, DBConstants.DownloadFiles.VersionKey)

  private def generateQueryConstraintClause[T](xs: Iterable[T], key: String)(implicit f: (T) => String): String =
    if (xs.isEmpty)
      ""
    else
      xs.map(x => s"""$key = "${f(x)}"""").mkString(" AND (", " OR ", ")")

  private def generateDateRangeMaybe[T <: Quantum[T]](start: T, end: T): VNel[Seq[T]] = {
    val range = start to end
    if (range.size > (365 * 2 + 1 + 1)) // 2 years + end date + possible leap day
      "Date range too large".failureNel[Seq[T]]
    else
      range.successNel[String]
  }

  def submit[T](submission: T)(implicit f: (T) => Submittable): ValidationNel[String, Long] = f(submission).submit
  def update[T](update: T)    (implicit f: (T) => Updatable):   Unit                        = f(update).update()

}

sealed trait Submittable {
  def submit: ValidationNel[String, Long]
}

private object Submittable {

  import AnormExtras.tryInsert

  implicit class UserDownloadSubmittable(userDownload: UserDownload) extends Submittable {
    override def submit: ValidationNel[String, Long] = DB.withConnection { implicit connection =>
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

  implicit class DownloadFileSubmittable(downloadFile: DownloadFile) extends Submittable {
    override def submit: ValidationNel[String, Long] = DB.withConnection { implicit connection =>
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

  implicit class UserDownloadUpdatable(userDownload: UserDownload) extends Updatable {
    override def update(): Unit = { DB.withConnection { implicit connection =>
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
        "file"  -> userDownload.file.id.get,
        "year"  -> userDownload.year,
        "month" -> userDownload.month,
        "day"   -> userDownload.day,
        "time"  -> userDownload.time
      )
      sql.executeUpdate()
    }}
  }

  implicit class DownloadFileUpdatable(downloadFile: DownloadFile) extends Updatable {
    override def update(): Unit = { DB.withConnection { implicit connection =>
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
  def timestamp(columnName: String): RowParser[Long] = get[JBigInt](columnName)(implicitly[Column[JBigInt]]) map (new BigInt(_).toLong)
  def raiseDBAccessException = throw new SQLException("Retrieved data from database in unexpected format.")
  def tryInsert(sql: SimpleSql[Row])(f: (Option[Long]) => ValidationNel[String, Long])
               (implicit connection: Connection): ValidationNel[String, Long] = {
    try f(sql.executeInsert())
    catch {
      case ex: MySQLIntegrityConstraintViolationException => s"SQL constraint violated: ${ex.getMessage}".failureNel[Long]
    }
  }
}

private object DBConstants {

  trait Table {
    def TableName: String
  }

  val CountKey = "count(*)"

  object UserDownloads extends Table {

    override lazy val TableName = "user_downloads"

    val DayKey    = "day"
    val FileIDKey = "file_id"
    val IDKey     = "id"
    val IPKey     = "ip"
    val MonthKey  = "month"
    val TimeKey   = "time"
    val YearKey   = "year"

  }

  object DownloadFiles extends Table {

    override lazy val TableName = "download_files"

    val IDKey      = "id"
    val OSKey      = "os"
    val PathKey    = "path"
    val SizeKey    = "size"
    val VersionKey = "version"

  }

}

