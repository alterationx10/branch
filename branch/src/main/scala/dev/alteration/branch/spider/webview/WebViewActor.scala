package dev.alteration.branch.spider.webview

import dev.alteration.branch.keanu.actors.{PoisonPill, StatefulActor}
import dev.alteration.branch.spider.websocket.WebSocketConnection
import dev.alteration.branch.spider.webview.devtools.{DevToolsState, UpdateDevToolsState}
import scala.util.{Failure, Success, Try}
import java.util.UUID

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
  * @param errorState
  *   Current error state (if any)
  * @param devToolsState
  *   DevTools state for this actor (if devMode enabled)
  * @param devToolsActorName
  *   The name of the DevTools actor to send updates to
  * @tparam State
  *   The user's state type
  */
private[webview] case class WebViewActorState[State](
    userState: Option[State],
    connection: Option[WebSocketConnection],
    mounted: Boolean,
    errorState: Option[ErrorState] = None,
    devToolsState: Option[DevToolsState] = None,
    devToolsActorName: Option[String] = None
)

/** Tracks error state for error boundaries. */
private[webview] case class ErrorState(
    error: Throwable,
    phase: ErrorPhase,
    attemptCount: Int = 0
)

/** Actor implementation for WebView.
  *
  * This actor manages the lifecycle of a WebView component, handling:
  *   - Connection establishment and mounting
  *   - Client events and state updates (with type-safe events)
  *   - Rendering and sending updates to the client
  *   - Clean termination
  *
  * Each connected client gets its own WebViewActor instance.
  *
  * @param webView
  *   The WebView implementation to manage
  * @tparam State
  *   The state type of the WebView
  * @tparam Event
  *   The event type of the WebView
  */
case class WebViewActor[State, Event](webView: WebView[State, Event])
    extends StatefulActor[WebViewActorState[State], WebViewMessage] {

  override def initialState: WebViewActorState[State] =
    WebViewActorState(None, None, false)

  override def statefulOnMsg
      : PartialFunction[WebViewMessage, WebViewActorState[State]] = {

    case Mount(params, session, connection, devToolsActorNameOpt) if !state.mounted =>
      // Initialize the WebView with error boundary
      handleWithErrorBoundary(ErrorPhase.Mount, None, connection) {
        val initialUserState = webView.mount(params, session)

        // Initialize DevTools state if devMode is enabled
        val viewId = UUID.randomUUID().toString
        val devToolsState = devToolsActorNameOpt.map { _ =>
          DevToolsState(viewId).recordMount(initialUserState)
        }

        // Call afterMount hook with context
        val ctx = WebViewContext(
          context.system,
          msg => context.system.tell(context.self, InfoMessage(msg))
        )

        Try(webView.afterMount(initialUserState, ctx)) match {
          case Failure(error) =>
            println(s"Error in afterMount: ${error.getMessage}")
            // Continue with mount even if afterMount fails
          case Success(_) => ()
        }

        // Render with beforeRender hook
        val renderState = webView.beforeRender(initialUserState)
        val html = webView.render(renderState)

        // Send initial HTML to client
        sendHtml(connection, html)

        val newState = state.copy(
          userState = Some(initialUserState),
          connection = Some(connection),
          mounted = true,
          errorState = None,
          devToolsState = devToolsState,
          devToolsActorName = devToolsActorNameOpt
        )

        // Send DevTools update
        sendDevToolsUpdate(newState)

        newState
      }

    case ClientEvent(typedEvent: Event @unchecked) if state.mounted =>
      // Handle client event with error boundary
      state.userState match {
        case Some(currentUserState) =>
          state.connection match {
            case Some(conn) =>
              handleWithErrorBoundary(ErrorPhase.Event, Some(currentUserState), conn) {
                val ctx = WebViewContext(
                  context.system,
                  msg => context.system.tell(context.self, InfoMessage(msg))
                )

                // Call beforeUpdate hook
                Try(webView.beforeUpdate(typedEvent, currentUserState, ctx)) match {
                  case Failure(error) =>
                    println(s"Error in beforeUpdate: ${error.getMessage}")
                  case Success(_) => ()
                }

                // Process the event
                val startTime = System.currentTimeMillis()
                val newUserState = webView.handleEvent(typedEvent, currentUserState)

                // Call afterUpdate hook
                Try(webView.afterUpdate(typedEvent, currentUserState, newUserState, ctx)) match {
                  case Failure(error) =>
                    println(s"Error in afterUpdate: ${error.getMessage}")
                  case Success(_) => ()
                }

                // Render with beforeRender hook
                val renderState = webView.beforeRender(newUserState)
                val html = webView.render(renderState)
                val renderTime = System.currentTimeMillis() - startTime

                // Send updated HTML to client
                sendHtml(conn, html)

                // Update DevTools state
                val newDevToolsState = state.devToolsState.map { devTools =>
                  devTools.recordEvent(typedEvent, currentUserState, newUserState, renderTime)
                }

                val newState = state.copy(
                  userState = Some(newUserState),
                  errorState = None,
                  devToolsState = newDevToolsState
                )

                // Send DevTools update
                sendDevToolsUpdate(newState)

                newState
              }
            case None =>
              println("Warning: Connection lost")
              state
          }

        case None =>
          println("Warning: Received event before mount")
          state
      }

    case InfoMessage(msg) if state.mounted =>
      // Handle info message with error boundary
      state.userState match {
        case Some(currentUserState) =>
          state.connection match {
            case Some(conn) =>
              handleWithErrorBoundary(ErrorPhase.Info, Some(currentUserState), conn) {
                val newUserState = webView.handleInfo(msg, currentUserState)

                // Render with beforeRender hook
                val renderState = webView.beforeRender(newUserState)
                val html = webView.render(renderState)

                // Send updated HTML to client
                sendHtml(conn, html)

                // Update DevTools state
                val newDevToolsState = state.devToolsState.map { devTools =>
                  devTools.recordInfo(msg, currentUserState, newUserState)
                }

                val newState = state.copy(
                  userState = Some(newUserState),
                  errorState = None,
                  devToolsState = newDevToolsState
                )

                // Send DevTools update
                sendDevToolsUpdate(newState)

                newState
              }
            case None =>
              println("Warning: Connection lost")
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

      // Update DevTools state with disconnect event
      val updatedDevToolsState = state.devToolsState.map { devTools =>
        import dev.alteration.branch.spider.webview.devtools.{ConnectionStatus, TimelineEntry}
        import java.time.Instant

        val disconnectEntry = TimelineEntry(
          timestamp = Instant.now(),
          eventType = "Disconnect",
          data = Map("message" -> "Client disconnected")
        )

        devTools
          .copy(timeline = devTools.timeline :+ disconnectEntry)
          .updateConnectionStatus(ConnectionStatus.Disconnected)
      }

      // Send final DevTools update
      val finalState = state.copy(devToolsState = updatedDevToolsState)
      sendDevToolsUpdate(finalState)

      // Close connection and stop actor
      state.connection.foreach(_.close())
      context.system.tell(context.self, PoisonPill)
      finalState.copy(connection = None, mounted = false)
  }

  /** Execute a block with error boundary protection.
    *
    * @param phase
    *   The phase where execution is happening
    * @param currentState
    *   Current user state (if available)
    * @param connection
    *   The WebSocket connection
    * @param block
    *   The block to execute
    * @return
    *   New actor state
    */
  private def handleWithErrorBoundary(
    phase: ErrorPhase,
    currentState: Option[State],
    connection: WebSocketConnection
  )(block: => WebViewActorState[State]): WebViewActorState[State] = {
    Try(block) match {
      case Success(newState) => newState

      case Failure(error) =>
        println(s"Error in ${phase.name}: ${error.getMessage}")
        error.printStackTrace()

        // Try to recover using error boundary
        currentState match {
          case Some(userState) =>
            // Call onError hook to attempt recovery
            Try(webView.onError(error, userState, phase)) match {
              case Success(Some(recoveredState)) =>
                println(s"Recovered from error in ${phase.name}")
                // Render recovered state
                Try {
                  val renderState = webView.beforeRender(recoveredState)
                  val html = webView.render(renderState)
                  sendHtml(connection, html)
                } match {
                  case Success(_) =>
                    state.copy(userState = Some(recoveredState), errorState = None)
                  case Failure(renderError) =>
                    println(s"Error rendering recovered state: ${renderError.getMessage}")
                    // Fall through to error UI
                    renderErrorUI(error, phase, connection)
                }

              case Success(None) | Failure(_) =>
                // No recovery possible or recovery failed, show error UI
                renderErrorUI(error, phase, connection)

            }

          case None =>
            // No state to recover from
            renderErrorUI(error, phase, connection)
        }
    }
  }

  /** Render and send error UI to the client.
    *
    * @param error
    *   The error that occurred
    * @param phase
    *   Where the error occurred
    * @param connection
    *   The WebSocket connection
    * @return
    *   Actor state with error recorded
    */
  private def renderErrorUI(
    error: Throwable,
    phase: ErrorPhase,
    connection: WebSocketConnection
  ): WebViewActorState[State] = {
    Try(webView.renderError(error, phase)) match {
      case Success(errorHtml) =>
        sendHtml(connection, errorHtml)
      case Failure(renderError) =>
        println(s"Error rendering error UI: ${renderError.getMessage}")
        // Send a basic error message
        val fallbackHtml = s"""
          <div style="padding: 20px; background: #fee; color: #c33;">
            <h2>Critical Error</h2>
            <p>Multiple errors occurred. Please reload the page.</p>
            <button onclick="location.reload()">Reload</button>
          </div>
        """
        sendHtml(connection, fallbackHtml)
    }

    // Update state with error
    state.copy(
      errorState = Some(ErrorState(error, phase, state.errorState.map(_.attemptCount + 1).getOrElse(1)))
    )
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

  /** Send DevTools update if devMode is enabled.
    *
    * @param actorState
    *   Current actor state containing DevTools state
    */
  private def sendDevToolsUpdate(actorState: WebViewActorState[State]): Unit = {
    for {
      devToolsActorName <- actorState.devToolsActorName
      devToolsState <- actorState.devToolsState
    } {
      Try {
        // Use tellPath to send to the DevTools actor by path
        val actorPath = s"/user/$devToolsActorName"
        context.system.tellPath(
          actorPath,
          InfoMessage(UpdateDevToolsState(devToolsState.viewId, devToolsState))
        )
      } match {
        case Failure(error) =>
          // DevTools actor might not exist yet if DevTools page isn't open
          // This is expected behavior, so we just silently ignore
          ()
        case Success(_) => ()
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
