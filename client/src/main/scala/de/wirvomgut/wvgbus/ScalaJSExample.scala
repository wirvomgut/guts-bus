package de.wirvomgut.wvgbus


import java.util.Date

import de.wirvomgut.wvgbus.shared.BusProtocol
import de.wirvomgut.wvgbus.shared.BusProtocol._
import org.scalajs.dom
import org.scalajs.dom.raw._
import upickle.default._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

object ScalaJSExample {
  val socket = new WebSocket(getWebsocketUri(dom.document))

  def main(args: Array[String]): Unit = {
    socket.onopen = { (event: Event) =>
      println(s"OnOpen -> $event")
      socket.send(write(BusConnect))
      event
    }

    socket.onerror = { (event: ErrorEvent) ⇒
      println(s"OnError -> $event")

      event
    }

    socket.onmessage = { (event: MessageEvent) ⇒
      println(s"OnMessage -> ${event.data.toString}")
      import de.wirvomgut.wvgbus.shared.BusProtocolPickler._

      val busMessage = read[BusProtocol.BusMessage](event.data.toString)

      matchBusMessages(busMessage)

      event
    }

    socket.onclose = { (event: CloseEvent) =>
      println(s"OnClose -> $event")

      addTableRow(SimpleMessage("Der Bus hat leider eine Panne. Lade die Seite neu für eine neue Fahrt!"))

      event
    }
  }

  def matchBusMessages(busMessage: BusMessage): Unit = {
    busMessage match {
      case m: FromToMessage => addFromToTableRow(m)
      case m: SimpleMessage => addTableRow(m)
      case m: MultipleMessages => m.messages.foreach(matchBusMessages)
    }
  }

  def addTableRow(msg: SimpleMessage): Unit = {
    addFromToTableRow(
      date = msg.received,
      msg = msg.message
    )
  }

  def addFromToTableRow(msg: FromToMessage): Unit = {
    addFromToTableRow(
      date = msg.simpleMessage.received,
      msg = msg.simpleMessage.message,
      from = msg.from,
      to = msg.to,
      action = s"""<p class="field is-grouped">
                            <a class="button is-small" onclick="turnOn('${msg.to}')">
                              <span>AN</span>
                            </a>
                            <a class="button is-small" onclick="turnOff('${msg.to}')">
                              <span>AUS</span>
                            </a>
                           </p>"""
    )
  }

  def addFromToTableRow(date: Date, msg: String, from: String = "", to: String = "", action: String = ""): Unit = {
    val table = dom.document.getElementById("bus").asInstanceOf[HTMLTableSectionElement]
    val row = table.insertRow(1).asInstanceOf[HTMLTableRowElement]

    // Insert new cells (<td> elements) at the 1st and 2nd position of the "new" <tr> element:
    val cell1 = row.insertCell(0)
    val cell2 = row.insertCell(1)
    val cell3 = row.insertCell(2)
    val cell4 = row.insertCell(3)
    val cell5 = row.insertCell(4)

    cell1.innerHTML = date.toString
    cell2.innerHTML = msg
    cell3.innerHTML = from
    cell4.innerHTML = to
    cell5.innerHTML = action
  }

  @JSExportTopLevel("turnOn")
  def turnOn(addr: String): Unit = {
    socket.send(write(TurnOn(addr)))
  }

  @JSExportTopLevel("turnOff")
  def turnOff(addr: String): Unit = {
    socket.send(write(TurnOff(addr)))
  }

  def getWebsocketUri(document: Document): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"

    s"$wsProtocol://${dom.document.location.host}/ws"
  }
}
