import play.api.{ Application => PlayApp, GlobalSettings }

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 7/11/13
 * Time: 2:38 PM
 */

object Global extends GlobalSettings {

  override def onStart(app: PlayApp): Unit = {
    controllers.Script.init()
  }

}
