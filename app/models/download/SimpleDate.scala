package models.download

import scala.annotation.tailrec

import org.joda.time.LocalDate

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 1/10/13
 * Time: 2:30 PM
 */

case class SimpleDate(day: Int, month: Int, year: Int) {

  def this(jodaDate: LocalDate) =
    this(jodaDate.getDayOfMonth, jodaDate.getMonthOfYear, jodaDate.getYear)

  def this(dateString: String)(implicit dmy: (Int, Int, Int) = SimpleDate.retrieveDMY(dateString)) =
    this(dmy._1, dmy._2, dmy._3)

  def <=(that: SimpleDate) : Boolean = {
    (this.year < that.year) || {
      (this.year == that.year) && ((this.month < that.month) || {
        (this.month == that.month) && (this.day <= that.day)
      })
    }
  }

  def to(that: SimpleDate) : Seq[SimpleDate] = {

    @tailrec
    def helper(startDate: LocalDate, endDate: LocalDate, acc: Seq[SimpleDate] = Seq()) : Seq[SimpleDate] = {
      if (startDate isBefore endDate)
        helper(startDate.plusDays(1), endDate, SimpleDate(startDate) +: acc)
      else
        (SimpleDate(startDate) +: acc).reverse
    }

    val (start, end) = if (this <= that) (this, that) else (that, this)
    helper(start.asJodaDate, end.asJodaDate)

  }

  def asDateString = "%d/%d/%d".format(this.month, this.day, this.year)

  def asJodaDate = new LocalDate(this.year, this.month, this.day)

}

object SimpleDate {

  def apply(jodaDate: LocalDate) = new SimpleDate(jodaDate)
  def apply(dateString: String)  = new SimpleDate(dateString)

  private def retrieveDMY(str: String) : (Int, Int, Int) = {
    val DateRegex = """(\d{1,2})[/-](\d{1,2})[/-](\d{2}|\d{4})""".r
    str match {
      case DateRegex(d, m, y) =>
        val day   = d.toInt
        val month = m.toInt
        val year  = (if (y.length == 2) "20" + y else y).toInt
        (day, month, year)
    }
  }

}
