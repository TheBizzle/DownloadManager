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
  case object Other   extends OS

  def parseOne(str: String) : OS = {
    if (str contains "Windows")
      Windows
    else if (str contains "Mac")
      Mac
    else if ((str contains "nix") || (str contains "nux"))
      Linux
    else
      Other
  }

  def parseMany(s: String) : Set[OS] = s split '|' map (parseOne(_)) toSet

}
