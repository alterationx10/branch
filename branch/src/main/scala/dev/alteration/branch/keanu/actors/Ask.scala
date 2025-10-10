package dev.alteration.branch.keanu.actors

import java.util.concurrent.TimeoutException
import scala.concurrent.Promise

/** Message wrapper for the ask pattern that enables request-response
  * communication.
  *
  * Actors should pattern match on this type to support request-response
  * messaging. When an actor receives an Ask, it should use the `complete` or
  * `fail` methods to respond.
  *
  * Example:
  * {{{
  * case class QueryActor() extends Actor {
  *   def onMsg: PartialFunction[Any, Any] = {
  *     case ask: Ask[?] =>
  *       ask.message match {
  *         case query: String =>
  *           val result = processQuery(query)
  *           ask.complete(result)
  *         case _ =>
  *           ask.fail(new IllegalArgumentException("Unknown message"))
  *       }
  *
  *     case query: String =>
  *       // Fire-and-forget version
  *       processQuery(query)
  *   }
  * }
  * }}}
  *
  * @param message
  *   The original message sent to the actor
  * @param promise
  *   The promise to complete with the response
  * @tparam R
  *   The expected response type
  */
case class Ask[R](message: Any, promise: Promise[R]) {

  /** Complete the promise with a successful response.
    *
    * This method handles the type casting internally, so actors don't need to
    * worry about the generic type parameter.
    *
    * @param response
    *   The response value to send back
    */
  def complete(response: Any): Unit = {
    promise.asInstanceOf[Promise[Any]].success(response)
  }

  /** Fail the promise with an error.
    *
    * @param error
    *   The error to send back
    */
  def fail(error: Throwable): Unit = {
    promise.failure(error)
  }
}

/** Exception thrown when an ask operation times out.
  *
  * @param message
  *   The error message
  * @param actorName
  *   The name of the actor that was asked
  * @param originalMessage
  *   The message that was sent
  */
class AskTimeoutException(
    message: String,
    val actorName: String,
    val originalMessage: Any
) extends TimeoutException(message)
