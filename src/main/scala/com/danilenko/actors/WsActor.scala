package com.danilenko.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, Unsubscribe}
import com.danilenko.actors.WsActor._
import com.danilenko.auth.AuthService
import com.danilenko.auth.AuthService.Permission.{AdminPermission, Permission}
import com.danilenko.service.TablesService
import com.danilenko.service.TablesService.{CreateTableRequest, Table}

class WsActor(authService: AuthService,
              tablesService: TablesService) extends Actor with ActorLogging {

  import context.dispatcher

  private val topic = "calculated-events"
  private val mediator = DistributedPubSub(context.system).mediator
  private var clientActor: Option[ActorRef] = None
  private var clientPermission: Option[Permission] = None

  override def receive: Receive = {

    case WsClientConnected(client) =>
      clientActor = Some(client)
      log.info(s"Client $client successfully connected to ws server")

    case Login(_, userName, password) => authService.login(userName, password).map {
      case Right(p: Permission) =>
        responseClientWith(LoginSuccessful(user_type = p.toString))
        clientPermission = Some(p)
        context.become(authorizedClient)

      case Left(_) => responseClientWith(LoginFailed())
    }

    case WsClientDisconnected => log.info("Client disconnected")

    case _ => responseClientWith(NotAuthorizedUser())

  }

  private def authorizedClient: Receive = {

    case a: Ping =>
      log.info(s"Ping message $a")
      responseClientWith(Pong(seq = a.seq))

    case _: SubscribeTables =>
      log.info("user subscribed to table updates")
      mediator ! Subscribe(topic, self)
      tablesService.get.map(tb => responseClientWith(TablesList(tables = tb)))

    case _: UnSubscribeTables =>
      log.info("user unSubscribed to table updates")
      mediator ! Unsubscribe(topic, self)

    case _: Unsubscribe =>
      log.info("client unsubscribed from tables updates ")

    case c: AddTable if isAuthorizedAdmin =>
      log.info("adding new table")
      tablesService.add(c.table).map { tb =>
       mediator ! Publish(topic, NewTableAddedEvent(after_id = c.after_id, table = tb))
      }

    case c: UpdateTable if isAuthorizedAdmin =>
      log.info(s"updating table ${c.table.id}")
      tablesService.update(c.table).map {
        case Some(updatedTable) => mediator ! Publish(topic, TableUpdatedEvent(table = updatedTable))
        case None => responseClientWith(TableUpdateFailed(id = c.table.id))
      }

    case c: RemoveTable if isAuthorizedAdmin =>
      log.info(s"removing table ${c.id}")
      tablesService.remove(c.id).map {
        case Some(removedTable) => mediator ! Publish(topic, WsActor.TableRemovedEvent(id = removedTable.id))
        case None => responseClientWith(TableRemoveFailed(id = c.id))
      }

    // success events
    case a: NewTableAddedEvent =>
      responseClientWith(a)

    case a: TableRemovedEvent =>
      responseClientWith(a)

    case a: TableUpdatedEvent =>
      responseClientWith(a)

    // failed events
    case a: TableRemoveFailed =>
      responseClientWith(a)

    case a: TableUpdateFailed =>
      responseClientWith(a)

    case otherCommand: BaseCommand =>
      log.info(s"other command $otherCommand")
      responseClientWith(NotAuthorizedUser())
  }

  private def isAuthorizedAdmin: Boolean = clientPermission.contains(AdminPermission)

  private def responseClientWith(value: Any): Unit = clientActor.foreach(_ ! value)

}

object WsActor {

  def props(authService: AuthService, tablesService: TablesService): Props =
    Props(new WsActor(authService, tablesService))

  sealed trait BaseCommand { def $type: String }
  // auth command
  final case class Login($type: String = "login", username: String, password: String) extends BaseCommand

  // client commands
  final case class SubscribeTables($type: String = "subscribe_tables") extends BaseCommand
  final case class UnSubscribeTables($type: String = "unsubscribe_tables") extends BaseCommand
  final case class AddTable($type: String = "add_table", after_id: Int, table: CreateTableRequest) extends BaseCommand
  final case class UpdateTable($type: String = "update_table", table: Table) extends BaseCommand
  final case class RemoveTable($type: String = "remove_table", id: Int) extends BaseCommand
  final case class Ping($type: String = "ping", seq: Int) extends BaseCommand

  sealed trait ServerEvent { def $type: String }

  // auth events
  final case class LoginSuccessful($type: String = "login_successful", user_type: String) extends ServerEvent
  final case class LoginFailed($type: String = "login_failed") extends ServerEvent
  final case class NotAuthorizedUser($type: String = "not_authorized") extends ServerEvent
  //ping event
  final case class Pong($type: String = "pong", seq: Int) extends ServerEvent
  //table events
  final case class TablesList($type: String = "table_list", tables: Seq[Table]) extends ServerEvent
  final case class TableRemoveFailed($type: String = "removal_failed", id: Int) extends ServerEvent
  final case class TableUpdateFailed($type: String = "update_failed", id: Int) extends ServerEvent
  final case class NewTableAddedEvent($type: String = "table_added", after_id: Int, table: Table) extends ServerEvent
  final case class TableRemovedEvent($type: String = "table_removed", id: Int) extends ServerEvent
  final case class TableUpdatedEvent($type: String = "table_updated", table: Table) extends ServerEvent

  // internal actor events
  final case class WsClientConnected(actorRef: ActorRef)
  final object WsClientDisconnected
}
