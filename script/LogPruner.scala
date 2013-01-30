import
  java.io.{ File, PrintWriter }

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 1/30/13
 * Time: 1:14 PM
 */

object LogPruner {

  def apply(file: File) {

    val outputFilePath = s"${file.getParent}${File.separator}pruned-${file.getName}"

    val logLines     = io.Source.fromFile(file).getLines()
    val outputWriter = new PrintWriter(outputFilePath)

    logLines foreach {
      case line if (isRelevant(line)) => outputWriter.println(line)
      case _                          => // Do nothing
    }
    outputWriter.close()

  }

  private def isRelevant(line: String) : Boolean = {
    (line.contains(".exe") || line.contains(".dmg") || line.contains(".tar.gz")) &&
      line.contains(" 200 ") &&
      !line.contains("\"HEAD ") &&
      line.contains(" /netlogo/")
  }

}
