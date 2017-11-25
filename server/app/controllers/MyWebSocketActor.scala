package controllers

import akka.actor._
import de.wirvomgut.wvgbus.shared.BusProtocol._
import play.api.libs.json.JsValue
import services.BusService
import upickle.default._

object MyWebSocketActor {
  def props(out: ActorRef, busService: BusService) = Props(new MyWebSocketActor(out, busService))
}


class MyWebSocketActor(out: ActorRef, busService: BusService) extends Actor {
  import de.wirvomgut.wvgbus.shared.BusProtocolPickler._

  def receive: PartialFunction[Any, Unit] = {
    case busSystemMessage: BusMessage =>
      out ! write(busSystemMessage)
    case msg: String =>
      println(msg)
      val busMsg = read[BusMessage](msg.toString)
      busMsg match {
        case BusConnect =>
          println("Client connected")
          busService.actorsToNotify += self
          val lastMessages = busService.lastMessages.filter(_ != null)
          if(lastMessages.nonEmpty) out ! write(MultipleMessages(lastMessages))
          out ! write(SimpleMessage("Du sitzt jetzt im Bus. Wir vom Gut wünschen eine gute Fahrt!"))
        case t: TurnOn =>
          busService.knxToolOn(t.addr)
        case t: TurnOff =>
          busService.knxToolOff(t.addr)
      }
  }

  override def postStop: Unit = {
    println(s"Removed it -> $out")
    busService.actorsToNotify -= self
  }
}
