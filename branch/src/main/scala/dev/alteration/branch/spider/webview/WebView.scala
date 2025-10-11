package dev.alteration.branch.spider.webview

/** Core trait for defining a WebView component.
  *
  * A WebView is a stateful, reactive UI component that lives on the server and
  * maintains state while sending updates to connected clients over WebSocket.
  *
  * This is Branch's answer to Phoenix LiveView - a LiveView-like solution that
  * provides real-time, stateful web UIs using server-side rendering.
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
  * Example with typed events (Phase 4b):
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
  *   The type of events this WebView handles (use String for backward compatibility)
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
    * changes, etc.) and sent to the server over WebSocket, then decoded
    * into strongly-typed events.
    *
    * The compiler enforces exhaustiveness checking when pattern matching
    * on sealed trait events, ensuring all event cases are handled.
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
