package models.download

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 12/17/12
 * Time: 4:16 PM
 */

sealed trait OS

object OS {
  case object Windows extends OS
  case object Mac     extends OS
  case object Linux   extends OS
}
