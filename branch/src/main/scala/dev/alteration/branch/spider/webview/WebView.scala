package dev.alteration.branch.spider.webview

import dev.alteration.branch.keanu.actors.ActorSystem

/** Core trait for defining a WebView component.
  *
  * A WebView is a stateful, reactive UI component that lives on the server and
  * maintains state while sending updates to connected clients over WebSocket.
  *
  * Example:
  * {{{
  * case class CounterView() extends WebView[CounterState]:
  *   override def mount(params: Map[String, String], session: Session): CounterState =
  *     CounterState(count = params.get("initial").map(_.toInt).getOrElse(0))
  *
  *   override def handleEvent(event: String, payload: Map[String, Any], state: CounterState): CounterState =
  *     event match
  *       case "increment" => state.copy(count = state.count + 1)
  *       case "decrement" => state.copy(count = state.count - 1)
  *       case _ => state
  *
  *   override def render(state: CounterState): String =
  *     s\"\"\"
  *       <div id="counter">
  *         <h1>Count: ${state.count}</h1>
  *         <button wv-click="increment">+</button>
  *         <button wv-click="decrement">-</button>
  *       </div>
  *     \"\"\"
  * }}}
  *
  * Example with typed events:
  * {{{
  * sealed trait CounterEvent derives EventCodec
  * case object Increment extends CounterEvent
  * case object Decrement extends CounterEvent
  *
  * case class CounterView() extends WebView[CounterState, CounterEvent]:
  *   override def handleEvent(event: CounterEvent, state: CounterState): CounterState =
  *     event match
  *       case Increment => state.copy(count = state.count + 1)
  *       case Decrement => state.copy(count = state.count - 1)
  *       // Compiler enforces exhaustiveness!
  * }}}
  *
  * @tparam State
  *   The type of state this WebView maintains
  * @tparam Event
  *   The type of events this WebView handles (use String for backward
  *   compatibility)
  */
trait WebView[State, Event] {

  /** Called when a client connects to this WebView.
    *
    * Use this to initialize your state based on URL parameters and session
    * data.
    *
    * @param params
    *   URL query parameters
    * @param session
    *   Session data (for authentication, user preferences, etc.)
    * @return
    *   The initial state
    */
  def mount(params: Map[String, String], session: Session): State

  /** Handle events sent from the client.
    *
    * Events are triggered by user interactions in the browser (clicks, form
    * changes, etc.) and sent to the server over WebSocket, then decoded into
    * strongly-typed events.
    *
    * The compiler enforces exhaustiveness checking when pattern matching on
    * sealed trait events, ensuring all event cases are handled.
    *
    * @param event
    *   The typed event (e.g., Increment, SetName("Alice"))
    * @param state
    *   The current state
    * @return
    *   The new state
    */
  def handleEvent(event: Event, state: State): State

  /** Handle info messages from the actor system.
    *
    * Use this for pub/sub updates, timers, messages from other actors, etc.
    *
    * @param msg
    *   The message received
    * @param state
    *   The current state
    * @return
    *   The new state
    */
  def handleInfo(msg: Any, state: State): State = state

  /** Render the current state as HTML.
    *
    * This HTML will be sent to the client and displayed in the browser. The
    * HTML should include WebView attributes (e.g., wv-click="event-name") to
    * wire up client-side events.
    *
    * @param state
    *   The current state
    * @return
    *   HTML string representing the current state
    */
  def render(state: State): String

  /** Called when the WebView terminates.
    *
    * Use this for cleanup (e.g., unsubscribing from pub/sub topics, closing
    * resources).
    *
    * @param reason
    *   The reason for termination (if any)
    * @param state
    *   The final state
    */
  def terminate(reason: Option[Throwable], state: State): Unit = {}

  // ===== Lifecycle Hooks =====

  /** Called immediately after the WebView is mounted with initial state.
    *
    * Use this to run side effects like:
    *   - Subscribing to pub/sub topics
    *   - Starting timers
    *   - Loading additional data asynchronously
    *
    * This hook has access to the WebView context, allowing you to send messages
    * to self or interact with the actor system.
    *
    * @param state
    *   The initial state after mount
    * @param context
    *   The WebView context for sending messages and interacting with actors
    */
  def afterMount(state: State, context: WebViewContext): Unit = {}

  /** Called before an event is processed.
    *
    * Use this for:
    *   - Logging/debugging events
    *   - Authorization checks
    *   - Pre-processing event data
    *
    * @param event
    *   The event about to be processed
    * @param state
    *   The current state before the event is applied
    * @param context
    *   The WebView context
    */
  def beforeUpdate(
      event: Event,
      state: State,
      context: WebViewContext
  ): Unit = {}

  /** Called after an event has been processed and state has been updated.
    *
    * Use this for:
    *   - Side effects based on state changes
    *   - Triggering async operations
    *   - Broadcasting updates to other actors
    *
    * @param event
    *   The event that was processed
    * @param oldState
    *   The state before the event was applied
    * @param newState
    *   The state after the event was applied
    * @param context
    *   The WebView context
    */
  def afterUpdate(
      event: Event,
      oldState: State,
      newState: State,
      context: WebViewContext
  ): Unit = {}

  /** Called before rendering, allowing state transformation.
    *
    * Use this for:
    *   - Adding computed/derived fields for rendering
    *   - Transforming state for display
    *   - Temporary view-specific state modifications
    *
    * Note: This should NOT have side effects. Only transform state for
    * rendering.
    *
    * @param state
    *   The current state
    * @return
    *   The transformed state (or same state if no transformation needed)
    */
  def beforeRender(state: State): State = state

  // ===== Error Boundary Hooks =====

  /** Called when an error occurs during event handling or rendering.
    *
    * This allows the WebView to recover from errors gracefully by returning a
    * new state. Return None to use the current state unchanged.
    *
    * Use this for:
    *   - Logging errors with context
    *   - Recovering to a safe state
    *   - Clearing problematic data
    *   - Setting error flags in state
    *
    * @param error
    *   The error that occurred
    * @param state
    *   The state when the error occurred
    * @param phase
    *   Where the error occurred (Mount, Event, Render, etc.)
    * @return
    *   Optional new state to recover to, or None to keep current state
    */
  def onError(
      error: Throwable,
      state: State,
      phase: ErrorPhase
  ): Option[State] = None

  /** Render an error UI when an unrecoverable error occurs.
    *
    * This is called when onError returns None or when the error boundary itself
    * throws an error. Override this to provide custom error UI.
    *
    * @param error
    *   The error that occurred
    * @param phase
    *   Where the error occurred
    * @return
    *   HTML to display the error (defaults to a simple error message)
    */
  def renderError(error: Throwable, phase: ErrorPhase): String = {
    val errorMsg = error.getMessage match {
      case null => error.getClass.getSimpleName
      case msg  => msg
    }

    s"""
    <div style="padding: 20px; margin: 20px; background: #fee; border: 2px solid #c33; border-radius: 8px; font-family: sans-serif;">
      <h2 style="color: #c33; margin: 0 0 10px 0;">⚠️ Error in ${phase.name}</h2>
      <p style="margin: 0 0 10px 0; color: #666;">
        Something went wrong. The error has been logged.
      </p>
      <details>
        <summary style="cursor: pointer; color: #666;">Error details</summary>
        <pre style="margin: 10px 0 0 0; padding: 10px; background: #f5f5f5; border-radius: 4px; overflow-x: auto; font-size: 0.875rem;">$errorMsg</pre>
      </details>
      <button onclick="location.reload()" style="margin-top: 15px; padding: 10px 20px; background: #667eea; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 1rem;">
        🔄 Reload Page
      </button>
    </div>
    """
  }

  /** Check if the WebView should attempt to retry after an error.
    *
    * Override this to implement custom retry logic.
    *
    * @param error
    *   The error that occurred
    * @param phase
    *   Where the error occurred
    * @param attemptCount
    *   Number of retry attempts so far
    * @return
    *   true if should retry, false otherwise
    */
  def shouldRetry(
      error: Throwable,
      phase: ErrorPhase,
      attemptCount: Int
  ): Boolean = false
}

/** Phase where an error occurred in the WebView lifecycle. */
enum ErrorPhase(val name: String) {
  case Mount     extends ErrorPhase("Mount")
  case Event     extends ErrorPhase("Event Handling")
  case Info      extends ErrorPhase("Info Handling")
  case Render    extends ErrorPhase("Rendering")
  case Lifecycle extends ErrorPhase("Lifecycle Hook")
}

/** Represents a client session.
  *
  * Contains session data like authentication tokens, user preferences, etc.
  */
case class Session(data: Map[String, Any] = Map.empty) {
  def get[A](key: String): Option[A] =
    data.get(key).map(_.asInstanceOf[A])

  def put(key: String, value: Any): Session =
    copy(data = data + (key -> value))

  def remove(key: String): Session =
    copy(data = data - key)
}

/** Context provided to lifecycle hooks.
  *
  * Allows WebViews to interact with the actor system during lifecycle events.
  * This enables patterns like pub/sub subscriptions and sending messages to
  * other actors.
  *
  * @param system
  *   The ActorSystem
  * @param sendSelfMsg
  *   Function to send a message to this WebView (will be received via
  *   handleInfo)
  */
case class WebViewContext(
    system: ActorSystem,
    private val sendSelfMsg: Any => Unit
) {

  /** Send a message to this WebView actor.
    *
    * The message will be received via handleInfo.
    *
    * @param msg
    *   The message to send
    */
  def sendSelf(msg: Any): Unit = {
    sendSelfMsg(msg)
  }

  /** Send a message to an actor by path.
    *
    * @param path
    *   The actor path (e.g., "/user/myactor")
    * @param msg
    *   The message to send
    */
  def tellPath(path: String, msg: Any): Unit = {
    system.tellPath(path, msg)
  }
}
