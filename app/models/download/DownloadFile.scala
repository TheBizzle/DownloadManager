package models.download

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 12/17/12
 * Time: 4:15 PM
 */

case class DownloadFile(id:      Option[Long] = None,
                        version: String,
                        os:      OS,
                        size:    Long,
                        path:    String)

