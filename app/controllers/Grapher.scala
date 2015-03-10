package controllers

import
  scala.util.Try

import
  scalaz.ValidationNel

import
  com.googlecode.charts4j._,
    Color.{ newColor, SKYBLUE, WHITE },
    Shape.DIAMOND

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 1/10/13
 * Time: 5:16 PM
 */

object Grapher {

  def fromStrCountPairsMaybe[N : Numeric](pairsMaybe: ValidationNel[String, Seq[(String, N)]]): String =
    pairsMaybe map {
      pairs => tryGenerateChartURL[N](pairs) map obtainChart getOrElse "../images/unscalable.png"
    } getOrElse "../images/too-many-days.png"

  private def obtainChart(urlStr: String): String = {

    import java.io.{ File, FileOutputStream }, java.net.URL, org.h2.util.IOUtils

    val filename = s"${java.util.UUID.randomUUID.toString}.png"
    val filepath = s"./public/graphs/$filename"
    val file     = new File(filepath)

    file.getParentFile.mkdir
    IOUtils.copyAndClose(new URL(urlStr).openStream(), new FileOutputStream(file))
    filename

  }

  private def tryGenerateChartURL[N : Numeric](dataPairs: Seq[(String, N)]): Try[String] = {

    def generateDownloadsLine(data: Seq[Double]): Line = {
      val max     = data.max
      val dataArr = if (max == 0) Data.newData(data.toArray: _*) else DataUtil.scaleWithinRange(0, max, data.toArray)
      val line    = Plots.newLine(dataArr, SKYBLUE, "Downloads")
      line.setLineStyle(LineStyle.newLineStyle(3, 1, 0))
      line.addShapeMarkers(DIAMOND, SKYBLUE, 12)
      line.addShapeMarkers(DIAMOND, WHITE, 8)
      line
    }

    def generateDownloadsChart(strs: Seq[String], maxDLCount: Int): (Line) => LineChart = {

      // They're "extra" because, a limit of `4` actually gets us 5 labels (the starting one and the four EXTRAS)
      // If we said in that case how many labels we wanted, it would be `5`, but, pretty much every time we use that number,
      // we would then need to subtract 1 from it. --JAB (1/14/13)
      val ExtraXLabelsLimit = 4
      val ExtraYLabelsLimit = 4

      def initChart(line: Line): LineChart = {
        val chart = GCharts.newLineChart(line)
        chart.setSize(600, 450) // Limit is 300K pixels; can't go much bigger than this
        chart.setTitle("NetLogo Downloads", WHITE, 24)
        chart.setGrid(100d / ExtraXLabelsLimit, 100d / ExtraYLabelsLimit, 3, 2)
        chart
      }

      def setupDownloadAxes(strs: Seq[String]): (LineChart) => LineChart = {

        val axisStyle = AxisStyle.newAxisStyle(WHITE, 12, AxisTextAlignment.CENTER)

        def createAxisLabelRange(start: Int, end: Int, extrasLimit: Int, endInclusive: Boolean = true) = {
          val trueEnd = if (endInclusive) end else end - 1
          if ((trueEnd - start) <= extrasLimit)
            start to trueEnd
          else
            (start to trueEnd by (end / extrasLimit) take extrasLimit) :+ trueEnd
        }

        def createXMarkers(xs: Seq[String])(chart: LineChart): LineChart = {
          val xStrs = createAxisLabelRange(0, xs.size, ExtraXLabelsLimit, false) map (xs(_))
          val xMarkers = AxisLabelsFactory.newAxisLabels(xStrs.toArray: _*)
          xMarkers.setAxisStyle(axisStyle)
          chart.addXAxisLabels(xMarkers)
          chart
        }

        def createXLabel(chart: LineChart): LineChart = {
          val xLabel = AxisLabelsFactory.newAxisLabels("Date", 50.0)
          xLabel.setAxisStyle(AxisStyle.newAxisStyle(WHITE, 16, AxisTextAlignment.CENTER))
          chart.addXAxisLabels(xLabel)
          chart
        }

        def createYMarkers(maxNum: Int)(chart: LineChart): LineChart = {
          val yStrs = createAxisLabelRange(0, maxDLCount, ExtraYLabelsLimit) map { case 0 => "" case x => x.toString }
          val yMarkers = AxisLabelsFactory.newAxisLabels(yStrs.toArray: _*)
          yMarkers.setAxisStyle(axisStyle)
          chart.addYAxisLabels(yMarkers)
          chart
        }

        def createYLabel(chart: LineChart): LineChart = {
          val yAxis2 = AxisLabelsFactory.newAxisLabels("Downloads", 50.0)
          yAxis2.setAxisStyle(AxisStyle.newAxisStyle(WHITE, 16, AxisTextAlignment.CENTER))
          chart.addYAxisLabels(yAxis2)
          chart
        }

        createXMarkers(strs) _ andThen createXLabel andThen createYMarkers(maxDLCount) andThen createYLabel

      }

      def setupBackground(chart: LineChart): LineChart = {
        chart.setBackgroundFill(Fills.newSolidFill(newColor("1F1D1D")))
        val fill = Fills.newLinearGradientFill(0, newColor("363433"), 100)
        fill.addColorAndOffset(Color.newColor("2E2B2A"), 0)
        chart.setAreaFill(fill)
        chart
      }

      initChart _ andThen setupDownloadAxes(strs) andThen setupBackground

    }

    def generateURLString(chart: AbstractAxisChart): String = chart.toURLString

    val (entities, counts) = dataPairs.unzip

    // This has been an interesting experiment --JAB (1/14/13)
    import Numeric.Implicits._
    Try((generateDownloadsLine _ andThen generateDownloadsChart(entities, counts.max.toInt) andThen generateURLString)(counts map (_.toDouble)))

  }

}
