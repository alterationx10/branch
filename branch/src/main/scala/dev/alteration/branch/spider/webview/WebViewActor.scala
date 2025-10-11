package dev.alteration.branch.spider.webview

import dev.alteration.branch.keanu.actors.{PoisonPill, StatefulActor}
import dev.alteration.branch.spider.websocket.WebSocketConnection
import scala.util.{Failure, Success, Try}

/** Internal state for the WebView actor.
  *
  * Wraps the user's state and includes metadata about the connection.
  *
  * @param userState
  *   The user's WebView state
  * @param connection
  *   The WebSocket connection (if mounted)
  * @param mounted
  *   Whether the WebView has been mounted
  * @tparam State
  *   The user's state type
  */
private[webview] case class WebViewActorState[State](
    userState: Option[State],
    connection: Option[WebSocketConnection],
    mounted: Boolean
)

/** Actor implementation for WebView.
  *
  * This actor manages the lifecycle of a WebView component, handling:
  *   - Connection establishment and mounting
  *   - Client events and state updates
  *   - Rendering and sending updates to the client
  *   - Clean termination
  *
  * Each connected client gets its own WebViewActor instance.
  *
  * @param webView
  *   The WebView implementation to manage
  * @tparam State
  *   The state type of the WebView
  */
case class WebViewActor[State](webView: WebView[State])
    extends StatefulActor[WebViewActorState[State], WebViewMessage] {

  override def initialState: WebViewActorState[State] =
    WebViewActorState(None, None, false)

  override def statefulOnMsg
      : PartialFunction[WebViewMessage, WebViewActorState[State]] = {

    case Mount(params, session, connection) if !state.mounted =>
      // Initialize the WebView
      Try {
        val initialUserState = webView.mount(params, session)
        val html             = webView.render(initialUserState)

        // Send initial HTML to client
        sendHtml(connection, html)

        state.copy(
          userState = Some(initialUserState),
          connection = Some(connection),
          mounted = true
        )
      } match {
        case Success(newState) => newState
        case Failure(error)    =>
          println(s"Error mounting WebView: ${error.getMessage}")
          sendError(error.getMessage)
          state.copy(connection = Some(connection))
      }

    case ClientEvent(event, payload) if state.mounted =>
      // Handle client event
      state.userState match {
        case Some(currentUserState) =>
          Try {
            val newUserState = webView.handleEvent(event, payload, currentUserState)
            val html         = webView.render(newUserState)

            // Send updated HTML to client
            state.connection.foreach { conn =>
              sendHtml(conn, html)
            }

            state.copy(userState = Some(newUserState))
          } match {
            case Success(newState) => newState
            case Failure(error)    =>
              println(s"Error handling event: ${error.getMessage}")
              sendError(error.getMessage)
              state
          }

        case None =>
          println("Warning: Received event before mount")
          state
      }

    case InfoMessage(msg) if state.mounted =>
      // Handle info message from actor system
      state.userState match {
        case Some(currentUserState) =>
          Try {
            val newUserState = webView.handleInfo(msg, currentUserState)
            val html         = webView.render(newUserState)

            // Send updated HTML to client
            state.connection.foreach { conn =>
              sendHtml(conn, html)
            }

            state.copy(userState = Some(newUserState))
          } match {
            case Success(newState) => newState
            case Failure(error)    =>
              println(s"Error handling info: ${error.getMessage}")
              sendError(error.getMessage)
              state
          }

        case None =>
          println("Warning: Received info before mount")
          state
      }

    case ClientDisconnected =>
      // Client disconnected, clean up
      if state.mounted then
        state.userState.foreach { userState =>
          webView.terminate(None, userState)
        }

      // Close connection and stop actor
      state.connection.foreach(_.close())
      context.system.tell(context.self, PoisonPill)
      state.copy(connection = None, mounted = false)
  }

  /** Send HTML to the client.
    *
    * @param conn
    *   The WebSocket connection
    * @param html
    *   The HTML to send
    */
  private def sendHtml(conn: WebSocketConnection, html: String): Unit = {
    val message = WebViewProtocol.ReplaceHtml(html)
    conn.sendText(message.toJson.toJsonString) match {
      case Success(_)     => // Success
      case Failure(error) =>
        println(s"Error: Failed to send HTML: ${error.getMessage}")
    }
  }

  /** Send an error message to the client.
    *
    * @param message
    *   The error message
    */
  private def sendError(message: String): Unit = {
    state.connection.foreach { conn =>
      val errorMsg = WebViewProtocol.Error(message)
      conn.sendText(errorMsg.toJson.toJsonString) match {
        case Success(_)     => // Success
        case Failure(error) =>
          println(s"Error: Failed to send error: ${error.getMessage}")
      }
    }
  }


  override def postStop(): Unit = {
    super.postStop()
    // Ensure cleanup on actor stop
    if state.mounted then
      state.userState.foreach { userState =>
        webView.terminate(None, userState)
      }
    state.connection.foreach(_.close())
  }

  override def preRestart(reason: Throwable): Unit = {
    super.preRestart(reason)
    // Notify WebView of failure
    if state.mounted then
      state.userState.foreach { userState =>
        webView.terminate(Some(reason), userState)
      }
  }
}
