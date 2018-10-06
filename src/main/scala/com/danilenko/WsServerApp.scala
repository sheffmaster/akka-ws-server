package com.danilenko

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.danilenko.auth.AuthServiceImpl
import com.danilenko.controller.WsController
import com.danilenko.service.TablesServiceImpl

import scala.io.StdIn

object WsServerApp extends App {

  implicit val system: ActorSystem = ActorSystem("ws-server-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  val authServiceImpl = new AuthServiceImpl
  val tablesServiceImpl = new TablesServiceImpl
  val wsController = new WsController(authServiceImpl, tablesServiceImpl)
  val apiRoutes: Route = Seq(wsController.route).reduce(_ ~ _)

  val bindingFuture = Http().bindAndHandle(apiRoutes, "localhost", 9000)

  println(s"Server online at http://localhost:9000/ws-api\nPress RETURN to stop...")
  StdIn.readLine()

  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
