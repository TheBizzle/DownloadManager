package controllers

import play.api.mvc.{ Action, Controller }

import models.download.DownloadDBManager

object Application extends Controller with Secured {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def downloads = withAuth {
    username => implicit request =>
      refreshPlots()
      Ok(views.html.downloads(username))
  }

  private def refreshPlots() {

    import java.text.SimpleDateFormat

    import org.joda.time.DateTime

    import org.jfree.chart.axis.{ DateAxis, DateTickUnit, DateTickUnitType }
    import scalala.library.Plotting._
    import scalala.tensor.dense.DenseVector

    val dt = new DateTime

    val startingYear   = 2002
    val startingMonth  = 1
    val startingDay    = 1
    val startingHour   = 0
    val startingMinute = 0

    val endingYear = dt.getYear

    val monthsInYear = dt.monthOfYear.getMaximumValue

    val yearMonthTuples = (startingYear until endingYear map (Seq.fill(monthsInYear)(_) zip (startingMonth to monthsInYear))).flatten
    val data = yearMonthTuples map { case (y, m) => (y, m, DownloadDBManager.getDLCountByYM(y, m)) }


    val startDate = new DateTime(startingYear, startingMonth, startingDay, startingHour, startingMinute)
    val endDate   = new DateTime(endingYear, startingMonth, startingDay, startingHour, startingMinute)

    val dateAxis = new DateAxis
    dateAxis.setDateFormatOverride(new SimpleDateFormat("MM/yy"))
    dateAxis.setRange(startDate.toDate, endDate.toDate)
    dateAxis.setTickUnit(new DateTickUnit(DateTickUnitType.YEAR, 1))

    plot.plot.setDomainAxis(dateAxis)

    val x = DenseVector(data map (_._3): _*)
    hist(x)
    figure.visible = false
    saveas("public/apples3.png")

  }

}
