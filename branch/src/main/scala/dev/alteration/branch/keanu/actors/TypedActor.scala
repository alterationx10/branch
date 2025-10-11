package dev.alteration.branch.keanu.actors

/** A typed actor that constrains messages to a specific type M.
  *
  * TypedActor extends Actor to provide compile-time type safety for message
  * handling. Instead of implementing onMsg with Any types, subclasses implement
  * typedOnMsg with a specific message type.
  *
  * Example:
  * {{{
  * sealed trait CounterMessage
  * case object Increment extends CounterMessage
  * case object Decrement extends CounterMessage
  *
  * case class CounterActor() extends TypedActor[CounterMessage]:
  *   var count: Int = 0
  *
  *   override def typedOnMsg: PartialFunction[CounterMessage, Any] = {
  *     case Increment => count += 1; count
  *     case Decrement => count -= 1; count
  *   }
  * }}}
  *
  * @tparam M
  *   The message type this actor handles
  */
trait TypedActor[M] extends Actor {

  /** Bridge to untyped Actor interface. This method is final and cannot be
    * overridden. Instead, implement typedOnMsg to handle typed messages.
    */
  final override def onMsg: PartialFunction[Any, Any] =
    new PartialFunction[Any, Any] {
      def isDefinedAt(x: Any): Boolean =
        try {
          typedOnMsg.isDefinedAt(x.asInstanceOf[M])
        } catch {
          case _: ClassCastException => false
        }

      def apply(x: Any): Any =
        typedOnMsg(x.asInstanceOf[M])
    }

  /** The typed message handler. Implement this method to handle messages of
    * type M.
    *
    * @return
    *   A partial function that handles messages of type M
    */
  def typedOnMsg: PartialFunction[M, Any]
}
