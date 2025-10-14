package dev.alteration.branch.spider.webview

import dev.alteration.branch.friday.Json
import dev.alteration.branch.friday.Json.*

/** Messages that a WebView actor can receive. */
sealed trait WebViewMessage

/** Lifecycle Messages */

/** Initialize the WebView with connection parameters.
  *
  * @param params
  *   URL query parameters
  * @param session
  *   Session data
  * @param connection
  *   The WebSocket connection
  * @param devToolsActorName
  *   Optional actor name for DevTools integration
  */
case class Mount(
    params: Map[String, String],
    session: Session,
    connection: dev.alteration.branch.spider.websocket.WebSocketConnection,
    devToolsActorName: Option[String] = None
) extends WebViewMessage

/** Event from the client.
  *
  * This message contains a strongly-typed event that has been decoded from the
  * client's JSON payload using an EventCodec.
  *
  * @param event
  *   The typed event (e.g., Increment, SetName("Alice"))
  * @tparam Event
  *   The event type
  */
case class ClientEvent[Event](event: Event) extends WebViewMessage

/** Message from the actor system (pub/sub, timers, etc.).
  *
  * @param msg
  *   The message payload
  */
case class InfoMessage(msg: Any) extends WebViewMessage

/** Client disconnected. */
case object ClientDisconnected extends WebViewMessage

/** Protocol messages exchanged between client and server over WebSocket. */
object WebViewProtocol {

  /** Messages sent from client to server */
  sealed trait ClientMessage

  /** Client is ready and requesting initial render */
  case object ClientReady extends ClientMessage

  /** Client triggered an event */
  case class Event(event: String, target: String, value: Option[Any])
      extends ClientMessage

  /** Client sent a heartbeat/ping */
  case object Heartbeat extends ClientMessage

  /** Messages sent from server to client */
  sealed trait ServerMessage {
    def toJson: Json
  }

  /** Replace entire HTML content */
  case class ReplaceHtml(html: String, target: String = "root")
      extends ServerMessage {
    def toJson: Json = Json.obj(
      "type"   -> JsonString("replace"),
      "html"   -> JsonString(html),
      "target" -> JsonString(target)
    )
  }

  /** Patch specific element */
  case class PatchHtml(html: String, target: String) extends ServerMessage {
    def toJson: Json = Json.obj(
      "type"   -> JsonString("patch"),
      "html"   -> JsonString(html),
      "target" -> JsonString(target)
    )
  }

  /** Send heartbeat response */
  case object HeartbeatResponse extends ServerMessage {
    def toJson: Json = Json.obj(
      "type" -> JsonString("pong")
    )
  }

  /** Error message */
  case class Error(message: String) extends ServerMessage {
    def toJson: Json = Json.obj(
      "type"    -> JsonString("error"),
      "message" -> JsonString(message)
    )
  }

  /** Parse a client message from JSON */
  def parseClientMessage(json: Json): Option[ClientMessage] =
    (json ? "type").strOpt match {
      case Some("ready")     => Some(ClientReady)
      case Some("event")     =>
        for {
          eventJson <- json ? "event"  // Event field is required
          target    <- (json ? "target").strOpt
          value      = (json ? "value")
        } yield {
          // Handle both string and JSON object events
          val event = eventJson match {
            case Json.JsonString(str)  => str
            case jobj: Json.JsonObject => jobj.toJsonString  // Pre-encoded event as object
            case j: Json               => j.toJsonString  // Any other JSON type
          }
          Event(event, target, value)
        }
      case Some("ping")      => Some(Heartbeat)
      case Some("heartbeat") => Some(Heartbeat)
      case _                 => None
    }

  /** Parse a client message from JSON string */
  def parseClientMessage(jsonStr: String): Option[ClientMessage] =
    Json.parse(jsonStr).toOption.flatMap(parseClientMessage)
}
