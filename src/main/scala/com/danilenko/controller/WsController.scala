package com.danilenko.controller

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.danilenko.actors.WsActor
import com.danilenko.actors.WsActor._
import com.danilenko.auth.AuthService
import com.danilenko.json.JsonProtocol._
import com.danilenko.service.TablesService
import io.circe.syntax._

class WsController(authService: AuthService,
                   tablesService: TablesService)
                  (implicit actorSystem: ActorSystem, materializer: ActorMaterializer) {

  val route: Route = path("ws-api") {
    handleWebSocketMessages(wsUser())
  }

  private def wsUser(): Flow[Message, Message, NotUsed] = {
    // Create an actor for every WebSocket connection
    val wsUser: ActorRef = actorSystem.actorOf(WsActor.props(authService, tablesService))

    // Integration point between Akka Streams and the above actor
    val sink: Sink[Message, NotUsed] =
      Flow[Message]
        .collect { case TextMessage.Strict(json) => decodeBaseCommand(json) }
        .collect { case Right(value) => value }
        .to(Sink.actorRef(wsUser, WsClientDisconnected)) // connect to the wsUser Actor

    // Integration point between Akka Streams and above actor
    val source: Source[Message, NotUsed] =
      Source
        .actorRef(bufferSize = 10, overflowStrategy = OverflowStrategy.dropBuffer)
        .map { c: ServerEvent => TextMessage.Strict(c.asJson.noSpaces) }
        .mapMaterializedValue { wsHandle =>
          wsUser ! WsClientConnected(wsHandle)
          // don't expose the wsHandle anymore
          NotUsed
        }

    Flow.fromSinkAndSource(sink, source)
  }

}

object WsController {
}
