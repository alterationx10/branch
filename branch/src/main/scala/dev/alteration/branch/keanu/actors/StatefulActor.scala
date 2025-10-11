package dev.alteration.branch.keanu.actors

/** A typed actor with built-in immutable state management.
  *
  * StatefulActor extends TypedActor to provide both type-safe message handling
  * and managed state. The state is maintained internally and updated through
  * the receive method, which returns a new state for each message.
  *
  * Example:
  * {{{
  * case class CounterState(count: Int, name: String)
  *
  * sealed trait CounterMessage
  * case object Increment extends CounterMessage
  * case object Decrement extends CounterMessage
  * case class SetName(name: String) extends CounterMessage
  *
  * case class CounterActor() extends StatefulActor[CounterState, CounterMessage]:
  *   override def initialState: CounterState =
  *     CounterState(0, "default")
  *
  *   override def receive: PartialFunction[CounterMessage, CounterState] = {
  *     case Increment => state.copy(count = state.count + 1)
  *     case Decrement => state.copy(count = state.count - 1)
  *     case SetName(n) => state.copy(name = n)
  *   }
  * }}}
  *
  * State Management:
  *   - State is initialized via initialState when the actor starts
  *   - Access current state via the protected state method
  *   - Return new state from receive to update it
  *   - State is reset to initialState on actor restart
  *
  * @tparam State
  *   The type of state this actor maintains
  * @tparam Msg
  *   The message type this actor handles
  */
trait StatefulActor[State, Msg] extends TypedActor[Msg] {

  /** Internal storage for actor state. Updated via receive method. */
  private var _state: State = initialState

  /** Protected accessor for current state. Use this in receive to access the
    * current state value.
    *
    * @return
    *   The current state
    */
  protected def state: State = _state

  /** Provide the initial state for this actor. Called when the actor starts
    * and when it restarts after a failure.
    *
    * If you need to preserve state across restarts, save it in preRestart and
    * restore it here:
    * {{{
    * private var savedState: Option[State] = None
    *
    * override def preRestart(reason: Throwable): Unit =
    *   savedState = Some(state)
    *
    * override def initialState: State =
    *   savedState.getOrElse(State.default)
    * }}}
    *
    * @return
    *   The initial state value
    */
  def initialState: State

  /** Handle messages and return new state. Access current state via the state
    * method.
    *
    * The new state you return will become the current state for subsequent
    * messages. If you want to keep the state unchanged, return state.
    *
    * @return
    *   A partial function that takes a message and returns new state
    */
  def receive: PartialFunction[Msg, State]

  /** Bridge to TypedActor. This method is final and updates internal state
    * based on the receive method.
    */
  final override def typedOnMsg: PartialFunction[Msg, Any] =
    new PartialFunction[Msg, Any] {
      def isDefinedAt(x: Msg): Boolean = receive.isDefinedAt(x)

      def apply(x: Msg): Any = {
        _state = receive(x)
        _state
      }
    }

  /** Reset state to initial value on restart. Override this if you need custom
    * restart behavior.
    */
  override def postRestart(reason: Throwable): Unit = {
    super.postRestart(reason)
    _state = initialState
  }
}
