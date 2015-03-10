package models.download

import
  scala.{ annotation, util },
    annotation.tailrec,
    util.matching.Regex

import
  org.joda.time.LocalDate

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 1/10/13
 * Time: 2:30 PM
 */

case class SimpleDate(day: Int, month: Int, year: Int) extends Quantum[SimpleDate] {

  def this(jodaDate: LocalDate) =
    this(jodaDate.getDayOfMonth, jodaDate.getMonthOfYear, jodaDate.getYear)

  def this(dateString: String)(implicit dmy: (Int, Int, Int) = SimpleDate.retrieveDMY(dateString)) =
    this(dmy._1, dmy._2, dmy._3)

  def toSimpleMonth: SimpleMonth =
    SimpleMonth(month, year)

  override protected val Companion = SimpleDate

  override def asDateString: String =
    s"${this.month}/${this.day}/${this.year}"

  override def asJodaDate: LocalDate =
    new LocalDate(this.year, this.month, this.day)

  override def <=(that: SimpleDate): Boolean = {
    (this.year < that.year) || {
      (this.year == that.year) && ((this.month < that.month) || {
        (this.month == that.month) && (this.day <= that.day)
      })
    }
  }

}

object SimpleDate extends QuantumCompanion[SimpleDate] {

  override def apply(jodaDate: LocalDate): SimpleDate = new SimpleDate(jodaDate)
  override def apply(dateString: String):  SimpleDate = new SimpleDate(dateString)

  override def incrementDate(jodaDate: LocalDate): LocalDate = jodaDate.plusDays(1)

  override lazy val DateRegex = """(\d{1,2})[/-](\d{1,2})[/-](\d{2}|\d{4})""".r

  private def retrieveDMY(str: String): (Int, Int, Int) = {
    val DateRegex(m, d, y) = str
    val day   = parseDayStr(d)
    val month = parseMonthStr(m)
    val year  = parseYearStr(y)
    (day, month, year)
  }

}



case class SimpleMonth(month: Int, year: Int) extends Quantum[SimpleMonth] {

  def this(jodaDate: LocalDate) =
    this(jodaDate.getMonthOfYear, jodaDate.getYear)

  def this(dateString: String)(implicit my: (Int, Int) = SimpleMonth.retrieveMY(dateString)) =
    this(my._1, my._2)

  def toSimpleYear: SimpleYear =
    SimpleYear(year)

  override protected val Companion = SimpleMonth

  override def asDateString: String =
    s"${this.month}/${this.year}"

  override def asJodaDate: LocalDate =
    new LocalDate(this.year, this.month, 1)

  override def <=(that: SimpleMonth): Boolean = {
    (this.year < that.year) || {
      (this.year == that.year) && (this.month <= that.month)
    }
  }

}

object SimpleMonth extends QuantumCompanion[SimpleMonth] {

  override def apply(jodaDate: LocalDate): SimpleMonth  = new SimpleMonth(jodaDate)
  override def apply(dateString: String):  SimpleMonth  = new SimpleMonth(dateString)

  override def incrementDate(jodaDate: LocalDate): LocalDate =
    jodaDate.plusMonths(1)

  override lazy val DateRegex = """(\d{1,2})[/-](\d{2}|\d{4})""".r

  private def retrieveMY(str: String): (Int, Int) = {
    val DateRegex(m, y) = str
    val month = parseMonthStr(m)
    val year  = parseYearStr(y)
    (month, year)
  }

}



case class SimpleYear(year: Int) extends Quantum[SimpleYear] {

  def this(jodaDate: LocalDate) =
    this(jodaDate.getYear)

  def this(dateString: String)(implicit y: Int = SimpleYear.retrieveY(dateString)) =
    this(y)

  override protected val Companion = SimpleYear

  override def asDateString: String =
    s"${this.year}"

  override def asJodaDate: LocalDate =
    new LocalDate(this.year, 1, 1)

  override def <=(that: SimpleYear): Boolean =
    this.year <= that.year

}

object SimpleYear extends QuantumCompanion[SimpleYear] {

  override def apply(jodaDate: LocalDate): SimpleYear = new SimpleYear(jodaDate)
  override def apply(dateString: String):  SimpleYear = new SimpleYear(dateString)

  override def incrementDate(jodaDate: LocalDate): LocalDate =
    jodaDate.plusYears(1)

  override lazy val DateRegex = """(\d{2}|\d{4})""".r

  private def retrieveY(str: String): Int = {
    val DateRegex(y) = str
    parseYearStr(y)
  }

}


trait Quantum[T <: Quantum[T]] {

  protected def Companion: QuantumCompanion[T]

  def <=(that: T): Boolean

  def asDateString: String
  def asJodaDate:   LocalDate

  def to(that: T): Seq[T] = {

    @tailrec
    def helper(startDate: LocalDate, endDate: LocalDate, acc: Seq[T] = Seq()): Seq[T] =
      if (startDate isBefore endDate)
        helper(Companion.incrementDate(startDate), endDate, Companion(startDate) +: acc)
      else
        (Companion(startDate) +: acc).reverse

    val (start, end) = if (this <= that) (this, that) else (that, this)
    helper(start.asJodaDate, end.asJodaDate)

  }

}

protected trait QuantumCompanion[T <: Quantum[T]] {

  def apply(jodaDate: LocalDate): T
  def apply(dateString: String):  T

  def incrementDate(jodaDate: LocalDate): LocalDate

  val DateRegex: Regex

  protected def parseDayStr  (s: String): Int = s.toInt
  protected def parseMonthStr(s: String): Int = s.toInt
  protected def parseYearStr (s: String): Int = (if (s.length == 2) "20" + s else s).toInt

}
