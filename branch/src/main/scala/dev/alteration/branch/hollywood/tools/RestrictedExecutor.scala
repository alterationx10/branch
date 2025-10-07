package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.friday.{Json, JsonDecoder}

import scala.util.{Failure, Success}

/** A generic ToolExecutor wrapper that enforces a ToolPolicy
  *
  * @param delegate
  *   The underlying tool executor
  * @param policy
  *   The security/validation policy to enforce
  * @param decoder
  *   JSON decoder for the tool type
  * @tparam T
  *   The CallableTool type
  */
class RestrictedExecutor[T <: CallableTool[?]](
    delegate: ToolExecutor[T],
    policy: ToolPolicy[T]
)(using decoder: JsonDecoder[T])
    extends ToolExecutor[T] {

  override def execute(args: Json): Json = {
    // Allow policy to transform args first (e.g., sanitize, add defaults)
    val transformedArgs = policy.transformArgs(args)

    // Decode the tool
    decoder.decode(transformedArgs) match {
      case Success(tool) =>
        // Validate against policy before executing
        policy.validate(tool) match {
          case Success(_) =>
            // Policy allows this operation - execute with transformed args
            delegate.execute(transformedArgs)
          case Failure(error) =>
            // Policy violation - return error
            Json.JsonString(s"Policy violation: ${error.getMessage}")
        }

      case Failure(error) =>
        Json.JsonString(s"Error decoding tool arguments: ${error.getMessage}")
    }
  }
}

object RestrictedExecutor {

  /** Create a RestrictedExecutor with a custom policy
    *
    * @param delegate
    *   The underlying tool executor
    * @param policy
    *   The policy to enforce
    * @param decoder
    *   JSON decoder for the tool type
    * @tparam T
    *   The CallableTool type
    * @return
    *   A new RestrictedExecutor instance
    */
  def apply[T <: CallableTool[?]](
      delegate: ToolExecutor[T],
      policy: ToolPolicy[T]
  )(using decoder: JsonDecoder[T]): RestrictedExecutor[T] =
    new RestrictedExecutor[T](delegate, policy)
}
