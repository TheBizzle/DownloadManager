package controllers

import models.download.Quantum

import com.googlecode.charts4j._
import Color._
import Shape._
import UrlUtil.normalize

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 1/10/13
 * Time: 5:16 PM
 */

object Grapher {

  def fromDateCountPairs[T <: Quantum[T], N : Numeric](pairs: Seq[(T, N)]) : String = (generateChartURL[T, N] _ andThen obtainChart _)(pairs)

  private def obtainChart(urlStr: String) : String = {
    import java.io.FileOutputStream, java.net.URL, org.h2.util.IOUtils
    val filename = java.util.UUID.randomUUID.toString + ".png"
    val filepath = "./public/graphs/%s".format(filename)
    IOUtils.copyAndClose(new URL(urlStr).openStream(), new FileOutputStream(filepath))
    filename
  }

  private def generateChartURL[T, N : Numeric](dataPairs: Seq[(T, N)]) : String = {

    def generateDownloadsLine(data: Seq[Double]) : Line = {
      val line = Plots.newLine(Data.newData(data.toArray: _*), SKYBLUE, "Downloads")
      line.setLineStyle(LineStyle.newLineStyle(3, 1, 0))
      line.addShapeMarkers(DIAMOND, SKYBLUE, 12)
      line.addShapeMarkers(DIAMOND, WHITE, 8)
      line
    }

    def setupDownloadAxes(ts: Seq[T], chart: AbstractAxisChart) {

      val axisStyle = AxisStyle.newAxisStyle(WHITE, 12, AxisTextAlignment.CENTER)

      val xAxis = AxisLabelsFactory.newAxisLabels("Nov", "Dec", "Jan", "Feb", "Mar")
      xAxis.setAxisStyle(axisStyle)

      val xAxis2 = AxisLabelsFactory.newAxisLabels("Month", 50.0)
      xAxis2.setAxisStyle(AxisStyle.newAxisStyle(WHITE, 14, AxisTextAlignment.CENTER))

      val yAxis = AxisLabelsFactory.newAxisLabels("", "25", "50", "75", "100")
      yAxis.setAxisStyle(axisStyle)

      val yAxis2 = AxisLabelsFactory.newAxisLabels("Hits", 50.0)
      yAxis2.setAxisStyle(AxisStyle.newAxisStyle(WHITE, 14, AxisTextAlignment.CENTER))

      chart.addXAxisLabels(xAxis)
      chart.addXAxisLabels(xAxis2)
      chart.addYAxisLabels(yAxis)
      chart.addYAxisLabels(yAxis2)

    }

    def setupBackground(chart: AbstractAxisChart) {
      chart.setBackgroundFill(Fills.newSolidFill(newColor("1F1D1D")))
      val fill = Fills.newLinearGradientFill(0, newColor("363433"), 100)
      fill.addColorAndOffset(Color.newColor("2E2B2A"), 0)
      chart.setAreaFill(fill)
    }

    def generateDownloadsChart(ts: Seq[T], dlLine: Line) : LineChart = {

      val chart = GCharts.newLineChart(dlLine)
      chart.setSize(600, 450) // Limit is 300K pixels; can't go much bigger than this
      chart.setTitle("NetLogo Downloads|(in thousands of downloads)", WHITE, 14)
      chart.setGrid(25, 25, 3, 2)

      setupDownloadAxes(ts, chart)
      setupBackground(chart)

      chart

    }

    def generateURLString(chart: AbstractAxisChart) = chart.toURLString

    val (entities, counts) = dataPairs.unzip

    // This has been an interesting experiment --JAB (1/14/13)
    import Numeric.Implicits._
    (generateDownloadsLine _ andThen generateDownloadsChart(entities, _) _  andThen generateURLString _)(counts map (_.toDouble))

  }

}
