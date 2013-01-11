package controllers

import models.download.SimpleDate

import com.googlecode.charts4j.{ AxisLabelsFactory, AxisStyle, AxisTextAlignment, Color, Data, Fills, GCharts, LineStyle, Plots, Shape, UrlUtil }
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

  def fromDateCountPairs(pairs: Seq[(SimpleDate, Long)]) : String = {
    makeChart()
  }
  
  private def makeChart() : String = {

    val NumPoints   = 25
    val competition = new Array[Double](NumPoints)
    val mywebsite   = new Array[Double](NumPoints)

    0 until NumPoints foreach {
      i =>
        competition(i) = 100 - (math.cos(30 * i * math.Pi / 180) * 10 + 50) * i / 20
        mywebsite(i)   = (math.cos(30 * i * math.Pi / 180) * 10 + 50) * i / 20
    }

    val line1 = Plots.newLine(Data.newData(mywebsite: _*), newColor("CA3D05"), "My Website.com")
    line1.setLineStyle(LineStyle.newLineStyle(3, 1, 0))
    line1.addShapeMarkers(DIAMOND, newColor("CA3D05"), 12)
    line1.addShapeMarkers(DIAMOND, WHITE, 8)

    val line2 = Plots.newLine(Data.newData(competition: _*), SKYBLUE, "Competition.com")
    line2.setLineStyle(LineStyle.newLineStyle(3, 1, 0))
    line2.addShapeMarkers(DIAMOND, SKYBLUE, 12)
    line2.addShapeMarkers(DIAMOND, WHITE, 8)

    // Defining chart.
    val chart = GCharts.newLineChart(line1, line2)
    chart.setSize(600, 450)
    chart.setTitle("Web Traffic|(in billions of hits)", WHITE, 14)
    chart.addHorizontalRangeMarker(40, 60, newColor(RED, 30))
    chart.addVerticalRangeMarker(70, 90, newColor(GREEN, 30))
    chart.setGrid(25, 25, 3, 2)

    // Defining axis info and styles
    val axisStyle = AxisStyle.newAxisStyle(WHITE, 12, AxisTextAlignment.CENTER)
    val xAxis = AxisLabelsFactory.newAxisLabels("Nov", "Dec", "Jan", "Feb", "Mar")
    xAxis.setAxisStyle(axisStyle)
    val xAxis2 = AxisLabelsFactory.newAxisLabels("2007", "2007", "2008", "2008", "2008")
    xAxis2.setAxisStyle(axisStyle)
    val yAxis = AxisLabelsFactory.newAxisLabels("", "25", "50", "75", "100")
    val xAxis3 = AxisLabelsFactory.newAxisLabels("Month", 50.0)
    xAxis3.setAxisStyle(AxisStyle.newAxisStyle(WHITE, 14, AxisTextAlignment.CENTER))
    yAxis.setAxisStyle(axisStyle)
    val yAxis2 = AxisLabelsFactory.newAxisLabels("Hits", 50.0)
    yAxis2.setAxisStyle(AxisStyle.newAxisStyle(WHITE, 14, AxisTextAlignment.CENTER))
    yAxis2.setAxisStyle(axisStyle)

    // Adding axis info to chart.
    chart.addXAxisLabels(xAxis)
    chart.addXAxisLabels(xAxis2)
    chart.addXAxisLabels(xAxis3)
    chart.addYAxisLabels(yAxis)
    chart.addYAxisLabels(yAxis2)

    // Defining background and chart fills.
    chart.setBackgroundFill(Fills.newSolidFill(newColor("1F1D1D")))
    val fill = Fills.newLinearGradientFill(0, newColor("363433"), 100)
    fill.addColorAndOffset(Color.newColor("2E2B2A"), 0)
    chart.setAreaFill(fill)

    chart.toURLString()

  }

}
