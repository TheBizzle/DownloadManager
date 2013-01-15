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

  def fromDateCountPairs[N : Numeric](pairs: Seq[(String, N)]) : String = (generateChartURL[N] _ andThen obtainChart _)(pairs)

  private def obtainChart(urlStr: String) : String = {
    import java.io.FileOutputStream, java.net.URL, org.h2.util.IOUtils
    val filename = java.util.UUID.randomUUID.toString + ".png"
    val filepath = "./public/graphs/%s".format(filename)
    IOUtils.copyAndClose(new URL(urlStr).openStream(), new FileOutputStream(filepath))
    filename
  }

  private def generateChartURL[N : Numeric](dataPairs: Seq[(String, N)]) : String = {

    def generateDownloadsLine(data: Seq[Double]) : Line = {
      val line = Plots.newLine(Data.newData(data.toArray: _*), SKYBLUE, "Downloads")
      line.setLineStyle(LineStyle.newLineStyle(3, 1, 0))
      line.addShapeMarkers(DIAMOND, SKYBLUE, 12)
      line.addShapeMarkers(DIAMOND, WHITE, 8)
      line
    }

    def generateDownloadsChart(strs: Seq[String])(dlLine: Line) : LineChart = {

      // They're "extra" because, a limit of `4` actually gets us 5 labels (the starting one and the four EXTRAS)
      // If we said in that case how many labels we wanted, it would be `5`, but, pretty much every time we use that number,
      // we would then need to subtract 1 from it. --JAB (1/14/13)
      val ExtraXLabelsLimit = 4
      val ExtraYLabelsLimit = 4

      def initChart(line: Line) : LineChart = {
        val chart = GCharts.newLineChart(dlLine)
        chart.setSize(600, 450) // Limit is 300K pixels; can't go much bigger than this
        chart.setTitle("NetLogo Downloads|(in thousands of downloads)", WHITE, 16)
        chart.setGrid(100d / ExtraXLabelsLimit, 100d / ExtraYLabelsLimit, 3, 2)
        chart
      }

      def setupDownloadAxes(strs: Seq[String])(chart: LineChart) : LineChart = {

        val axisStyle = AxisStyle.newAxisStyle(WHITE, 12, AxisTextAlignment.CENTER)

        def createAxisLabelRange(start: Int, end: Int, extrasLimit: Int, inclusive: Boolean = true) = {
          val trueEnd = if (inclusive) end else end - 1
          (start to trueEnd by (end / extrasLimit) dropRight 1) :+ trueEnd
        }

        val xStrs = createAxisLabelRange(0, strs.size, ExtraXLabelsLimit, false) map (strs(_))
        val xAxis = AxisLabelsFactory.newAxisLabels(xStrs.toArray: _*)
        xAxis.setAxisStyle(axisStyle)

        val xAxis2 = AxisLabelsFactory.newAxisLabels("Date", 50.0)
        xAxis2.setAxisStyle(AxisStyle.newAxisStyle(WHITE, 16, AxisTextAlignment.CENTER))

        val yStrs = createAxisLabelRange(0, 100, ExtraYLabelsLimit) map { case 0 => "" case x => x.toString }
        val yAxis = AxisLabelsFactory.newAxisLabels(yStrs.toArray: _*)
        yAxis.setAxisStyle(axisStyle)

        val yAxis2 = AxisLabelsFactory.newAxisLabels("Downloads", 50.0)
        yAxis2.setAxisStyle(AxisStyle.newAxisStyle(WHITE, 16, AxisTextAlignment.CENTER))

        chart.addXAxisLabels(xAxis)
        chart.addXAxisLabels(xAxis2)
        chart.addYAxisLabels(yAxis)
        chart.addYAxisLabels(yAxis2)

        chart

      }

      def setupBackground(chart: LineChart) : LineChart = {
        chart.setBackgroundFill(Fills.newSolidFill(newColor("1F1D1D")))
        val fill = Fills.newLinearGradientFill(0, newColor("363433"), 100)
        fill.addColorAndOffset(Color.newColor("2E2B2A"), 0)
        chart.setAreaFill(fill)
        chart
      }

      (initChart _ andThen setupDownloadAxes(strs) _ andThen setupBackground _)(dlLine)

    }

    def generateURLString(chart: AbstractAxisChart) = chart.toURLString

    val (entities, counts) = dataPairs.unzip

    // This has been an interesting experiment --JAB (1/14/13)
    import Numeric.Implicits._
    (generateDownloadsLine _ andThen generateDownloadsChart(entities) _ andThen generateURLString _)(counts map (_.toDouble))

  }

}
