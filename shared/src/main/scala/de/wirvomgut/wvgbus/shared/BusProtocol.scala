package de.wirvomgut.wvgbus.shared

import java.util.Date

import upickle.Js


object BusProtocolPickler {
  implicit val dateWriter = upickle.default.Writer[Date]{
    case d => Js.Num(d.getTime)
  }
  implicit val dateReader = upickle.default.Reader[Date]{
    case Js.Num(d) => new Date(d.toLong)
  }
}

object BusProtocol {
  sealed trait BusMessage
  case object BusConnect extends BusMessage
  case class SimpleMessage(message: String, received: Date = new Date()) extends BusMessage
  case class FromToMessage(simpleMessage: SimpleMessage, from: String, to: String) extends BusMessage
  case class MultipleMessages(messages: Seq[BusMessage]) extends BusMessage
  case class TurnOn(addr: String) extends BusMessage
  case class TurnOff(addr: String) extends BusMessage
}