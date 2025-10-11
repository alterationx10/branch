package dev.alteration.branch.spider.webview

import dev.alteration.branch.keanu.actors.ActorSystem
import dev.alteration.branch.spider.websocket.{WebSocketConnection, WebSocketHandler}
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** WebSocket handler that manages WebView connections.
  *
  * This handler creates a WebViewActor for each connected client and routes
  * messages between the WebSocket and the actor.
  *
  * Example usage:
  * {{{
  * val actorSystem = ActorSystem()
  * val handler = WebViewHandler(actorSystem, () => MyWebView())
  *
  * val server = WebSocketServer(8080)
  * server.addRoute("/webview", handler)
  * }}}
  *
  * @param actorSystem
  *   The actor system to use for spawning actors
  * @param webViewFactory
  *   Factory function to create WebView instances
  * @param params
  *   URL query parameters to pass to mount
  * @param session
  *   Session data to pass to mount
  * @tparam State
  *   The state type of the WebView
  */
class WebViewHandler[State](
    actorSystem: ActorSystem,
    webViewFactory: () => WebView[State],
    params: Map[String, String] = Map.empty,
    session: Session = Session()
) extends WebSocketHandler {

  // Map to store actor names by connection (using object identity)
  private val connectionActors =
    new ConcurrentHashMap[WebSocketConnection, String]()

  override def onConnect(connection: WebSocketConnection): Unit = {
    // Generate unique actor name for this connection
    val actorId = UUID.randomUUID().toString

    try {
      // Create the WebView instance
      val webView = webViewFactory()

      // Register actor props (for actor creation)
      actorSystem.registerProp(
        dev.alteration.branch.keanu.actors.ActorProps.props[WebViewActor[State]](
          Tuple1(webView)
        )
      )

      // Spawn the actor
      val actorName = s"webview-$actorId"
      actorSystem.actorOf[WebViewActor[State]](actorName)

      // Store actor name mapped to connection
      connectionActors.put(connection, actorName)

      // Send mount message to initialize (includes connection)
      actorSystem.tell[WebViewActor[State]](actorName, Mount(params, session, connection))

    } catch {
      case error: Throwable =>
        println(s"Error creating WebView actor: ${error.getMessage}")
        error.printStackTrace()
        connection.close()
    }
  }

  override def onMessage(connection: WebSocketConnection, message: String): Unit = {
    try {
      // Get actor name from connection
      val actorNameOpt = getActorName(connection)

      actorNameOpt match {
        case Some(actorName) =>
          // Parse the message
          WebViewProtocol.parseClientMessage(message) match {
            case Some(WebViewProtocol.ClientReady) =>
              // Client is ready, mount already happened in onConnect
              ()

            case Some(WebViewProtocol.Event(event, target, value)) =>
              // Extract payload
              val payload = Map(
                "target" -> target,
                "value"  -> value.getOrElse(null)
              )
              actorSystem.tell[WebViewActor[State]](actorName, ClientEvent(event, payload))

            case Some(WebViewProtocol.Heartbeat) =>
              // Send heartbeat response
              connection.sendText(
                WebViewProtocol.HeartbeatResponse.toJson.toJsonString
              )

            case None =>
              println(s"Failed to parse client message: $message")
          }

        case None =>
          println("No actor found for connection")
          connection.close()
      }
    } catch {
      case error: Throwable =>
        println(s"Error handling message: ${error.getMessage}")
        error.printStackTrace()
    }
  }

  override def onClose(
      connection: WebSocketConnection,
      statusCode: Option[Int],
      reason: String
  ): Unit = {
    // Notify actor that client disconnected
    getActorName(connection).foreach { actorName =>
      actorSystem.tell[WebViewActor[State]](actorName, ClientDisconnected)
    }
    // Clean up the mapping
    connectionActors.remove(connection)
  }

  override def onError(connection: WebSocketConnection, error: Throwable): Unit = {
    println(s"WebSocket error: ${error.getMessage}")
    error.printStackTrace()

    // Notify actor of disconnection
    getActorName(connection).foreach { actorName =>
      actorSystem.tell[WebViewActor[State]](actorName, ClientDisconnected)
    }
    // Clean up the mapping
    connectionActors.remove(connection)
  }

  /** Extract the actor name for this connection. */
  private def getActorName(connection: WebSocketConnection): Option[String] =
    Option(connectionActors.get(connection))
}

object WebViewHandler {

  /** Create a WebViewHandler with the given parameters.
    *
    * @param actorSystem
    *   The actor system to use
    * @param webViewFactory
    *   Factory function to create WebView instances
    * @param params
    *   URL query parameters
    * @param session
    *   Session data
    * @tparam State
    *   The WebView state type
    * @return
    *   A new WebViewHandler instance
    */
  def apply[State](
      actorSystem: ActorSystem,
      webViewFactory: () => WebView[State],
      params: Map[String, String] = Map.empty,
      session: Session = Session()
  ): WebViewHandler[State] =
    new WebViewHandler(actorSystem, webViewFactory, params, session)
}
