package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.friday.Json

import scala.util.{Failure, Success, Try}

/** Generic policy for restricting tool execution
  *
  * @tparam T
  *   The CallableTool type this policy applies to
  */
trait ToolPolicy[T <: CallableTool[?]] {

  /** Validate whether a tool invocation is allowed
    *
    * @param tool
    *   The decoded tool instance to validate
    * @return
    *   Success if allowed, Failure with SecurityException if not
    */
  def validate(tool: T): Try[Unit]

  /** Optional: Transform the tool arguments before validation/execution This
    * allows policies to modify requests (e.g., sanitize inputs, add defaults)
    *
    * @param args
    *   The raw JSON arguments
    * @return
    *   Transformed JSON arguments
    */
  def transformArgs(args: Json): Json = args
}

object ToolPolicy {

  /** A permissive policy that allows all operations */
  def allowAll[T <: CallableTool[?]]: ToolPolicy[T] = new ToolPolicy[T] {
    def validate(tool: T): Try[Unit] = Success(())
  }

  /** A restrictive policy that blocks all operations */
  def denyAll[T <: CallableTool[?]](reason: String = "All operations blocked by policy"): ToolPolicy[T] =
    new ToolPolicy[T] {
      def validate(tool: T): Try[Unit] =
        Failure(new SecurityException(reason))
    }

  /** Create a custom policy from a validation function */
  def fromValidator[T <: CallableTool[?]](
      validator: T => Try[Unit]
  ): ToolPolicy[T] = new ToolPolicy[T] {
    def validate(tool: T): Try[Unit] = validator(tool)
  }

  /** Create a custom policy with both validation and transformation */
  def custom[T <: CallableTool[?]](
      validator: T => Try[Unit],
      transformer: Json => Json = identity
  ): ToolPolicy[T] = new ToolPolicy[T] {
    def validate(tool: T): Try[Unit]   = validator(tool)
    override def transformArgs(args: Json): Json = transformer(args)
  }
}
