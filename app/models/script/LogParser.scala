/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 1/30/13
 * Time: 3:41 PM
 */

object LogParser {

  def apply(line: String) {

    val refuseCatcher = """.*""" // Four of the log entries abruptly cut off other entries; this ignores the cut-off junk at the beginning
    val ipRegex       = """(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})"""
    val spacerRegex   = """ - (?:-|ccl) """ // Useless spacer thing
    val dateRegex     = """\[([^ ]*) -0[56]00\]"""
    val urlRegex      = """ "\w+ /netlogo/([^/]*)/([^ ]*) HTTP/1.[01]""""
    val statusRegex   = """ 200 """
    val sizeRegex     = """(\d+)"""

    val regexes = Seq(refuseCatcher, ipRegex, spacerRegex, dateRegex, urlRegex, statusRegex, sizeRegex)

    val FullRegex = regexes reduceLeft (_ + _) r

    line match {
      case FullRegex(ip, date, version, filename, size) =>
        // Meh
      case _ =>
        println("Unmatchable download entry: " + line)
    }

  }

}
