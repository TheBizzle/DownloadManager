package models.download

/**
 * Created with IntelliJ IDEA.
 * User: jason
 * Date: 1/16/13
 * Time: 4:33 PM
 */

sealed trait GraphType

object GraphType {

  case object Discrete   extends GraphType
  case object Cumulative extends GraphType

  def apply(s: String) : GraphType = {
    s.toLowerCase match {
      case "discrete"   => Discrete
      case "cumulative" => Cumulative
      case _            => throw new Exception("Graph Boom!") //@
    }
  }

}
