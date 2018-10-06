package com.danilenko.json

import com.danilenko.actors.WsActor._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.{Decoder, Encoder, Error}

object JsonProtocol {

  implicit val baseCommandEncoder: Encoder[ServerEvent] = Encoder.instance[ServerEvent] {
    case a: NotAuthorizedUser => Encoder[NotAuthorizedUser].apply(a)
    case a: Pong => Encoder[Pong].apply(a)
    case a: TablesList => Encoder[TablesList].apply(a)
    case a: TableRemoveFailed => Encoder[TableRemoveFailed].apply(a)
    case a: TableUpdateFailed => Encoder[TableUpdateFailed].apply(a)
    case a: NewTableAddedEvent => Encoder[NewTableAddedEvent].apply(a)
    case a: TableRemovedEvent => Encoder[TableRemovedEvent].apply(a)
    case a: TableUpdatedEvent => Encoder[TableUpdatedEvent].apply(a)
    case a: LoginSuccessful => Encoder[LoginSuccessful].apply(a)
    case a: LoginFailed => Encoder[LoginFailed].apply(a)
  }

  def decodeBaseCommand(json: String): Either[Error, Any] = {
    val commandByBaseTypeDecoder = Decoder[String].prepare(_.downField("$type"))
    decode(json)(commandByBaseTypeDecoder).flatMap {
      case "login" => decode[Login](json)
      case "ping" => decode[Ping](json)
      case "subscribe_tables" => decode[SubscribeTables](json)
      case "unsubscribe_tables" => decode[UnSubscribeTables](json)
      case "add_table" => decode[AddTable](json)
      case "update_table" => decode[UpdateTable](json)
      case "remove_table" => decode[RemoveTable](json)
    }
  }

}
