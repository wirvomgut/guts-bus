package controllers

import javax.inject._

import akka.actor.ActorSystem
import akka.stream.Materializer
import de.wirvomgut.wvgbus.shared.SharedMessages
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import services.BusService

@Singleton
class Application @Inject()(cc: ControllerComponents, busService: BusService) (implicit system: ActorSystem, mat: Materializer) extends AbstractController(cc) {

  def index = Action { implicit request =>
    Ok(views.html.index(SharedMessages.itWorks))
  }

  def ws(): WebSocket = WebSocket.accept[String, String] { implicit request =>
    ActorFlow.actorRef(out => MyWebSocketActor.props(out, busService))
  }

}
