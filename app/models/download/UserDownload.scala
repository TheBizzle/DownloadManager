package models.download

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 12/17/12
 * Time: 4:12 PM
 */

case class UserDownload(id:    Option[Long] = None,
                        ip:    String,
                        file:  DownloadFile,
                        year:  Int,
                        month: Int,
                        day:   Int,
                        time:  String)

