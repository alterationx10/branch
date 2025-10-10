package dev.alteration.branch.blammo

import dev.alteration.branch.friday.Json
import dev.alteration.branch.friday.Json.*

import java.util.UUID
import scala.util.{Try, Success, Failure}

/** Context for a single span in a distributed trace
  * @param traceId
  *   The unique identifier for the entire trace
  * @param spanId
  *   The unique identifier for this span
  * @param parentSpanId
  *   The span ID of the parent span, if any
  */
case class SpanContext(
    traceId: String,
    spanId: String,
    parentSpanId: Option[String] = None
)

/** A trait for adding distributed tracing capabilities via structured logging.
  * Spans are logged as JSON events that can be collected and reconstructed by
  * log aggregators (Loki, CloudWatch, OpenSearch, etc).
  *
  * Example usage:
  * {{{
  * object MyService extends JsonConsoleLogger with Tracer {
  *   def handleRequest(req: Request): Response = traced("http.request") {
  *     val user = traced("db.query") { fetchUser() }
  *     traced("render.response") { buildResponse(user) }
  *   }
  * }
  * }}}
  */
trait Tracer { self: BaseLogger =>

  private val spanContext = new ThreadLocal[Option[SpanContext]] {
    override def initialValue(): Option[SpanContext] = None
  }

  /** Get the current span context, if any
    * @return
    *   The current span context or None
    */
  def currentSpan: Option[SpanContext] = spanContext.get()

  /** Start a new span within the current trace context
    * @param operation
    *   The operation name for this span
    * @param attributes
    *   Optional key-value attributes to attach to the span
    * @return
    *   The new span context
    */
  def startSpan(
      operation: String,
      attributes: Map[String, Json] = Map.empty
  ): SpanContext = {
    val parent = spanContext.get()
    val ctx    = SpanContext(
      traceId = parent.map(_.traceId).getOrElse(UUID.randomUUID().toString),
      spanId = UUID.randomUUID().toString,
      parentSpanId = parent.map(_.spanId)
    )
    spanContext.set(Some(ctx))

    val baseFields = Map(
      "event"     -> JsonString("span.start"),
      "trace_id"  -> JsonString(ctx.traceId),
      "span_id"   -> JsonString(ctx.spanId),
      "operation" -> JsonString(operation),
      "timestamp" -> JsonNumber(System.currentTimeMillis().toDouble)
    )

    val parentField =
      ctx.parentSpanId.map(id => "parent_span_id" -> JsonString(id)).toMap

    val allFields = baseFields ++ parentField ++ attributes

    logger.info(Json.obj(allFields.toSeq*).toJsonString)

    ctx
  }

  /** End a span and log its completion
    * @param ctx
    *   The span context to end
    * @param success
    *   Whether the span completed successfully
    * @param attributes
    *   Optional key-value attributes to attach to the span completion
    */
  def endSpan(
      ctx: SpanContext,
      success: Boolean = true,
      attributes: Map[String, Json] = Map.empty
  ): Unit = {
    val baseFields = Map(
      "event"     -> JsonString("span.end"),
      "trace_id"  -> JsonString(ctx.traceId),
      "span_id"   -> JsonString(ctx.spanId),
      "success"   -> JsonBool(success),
      "timestamp" -> JsonNumber(System.currentTimeMillis().toDouble)
    )

    val allFields = baseFields ++ attributes

    logger.info(Json.obj(allFields.toSeq*).toJsonString)

    // Clear current span context (parent restoration not yet implemented)
    spanContext.set(None)
  }

  /** Execute a block of code within a traced span
    * @param operation
    *   The operation name for this span
    * @param attributes
    *   Optional key-value attributes to attach to the span
    * @param block
    *   The code block to execute within the span
    * @tparam T
    *   The return type of the block
    * @return
    *   The result of executing the block
    */
  def traced[T](
      operation: String,
      attributes: Map[String, Json] = Map.empty
  )(block: => T): T = {
    val ctx       = startSpan(operation, attributes)
    val startTime = System.nanoTime()

    Try(block) match {
      case Success(result) =>
        val duration = (System.nanoTime() - startTime) / 1_000_000.0 // ms
        endSpan(
          ctx,
          success = true,
          attributes = attributes ++ Map(
            "duration_ms" -> JsonNumber(duration)
          )
        )
        result

      case Failure(error) =>
        val duration = (System.nanoTime() - startTime) / 1_000_000.0 // ms
        endSpan(
          ctx,
          success = false,
          attributes = attributes ++ Map(
            "duration_ms" -> JsonNumber(duration),
            "error"       -> JsonString(error.getMessage),
            "error_type"  -> JsonString(error.getClass.getName)
          )
        )
        throw error
    }
  }

  /** Execute a block of code within a traced span, with explicit success/failure
    * handling based on the Try result
    * @param operation
    *   The operation name for this span
    * @param attributes
    *   Optional key-value attributes to attach to the span
    * @param block
    *   The code block to execute within the span
    * @tparam T
    *   The return type of the block
    * @return
    *   A Try containing the result of executing the block
    */
  def tracedTry[T](
      operation: String,
      attributes: Map[String, Json] = Map.empty
  )(block: => T): Try[T] = {
    val ctx       = startSpan(operation, attributes)
    val startTime = System.nanoTime()

    Try(block) match {
      case success @ Success(_) =>
        val duration = (System.nanoTime() - startTime) / 1_000_000.0 // ms
        endSpan(
          ctx,
          success = true,
          attributes = attributes ++ Map(
            "duration_ms" -> JsonNumber(duration)
          )
        )
        success

      case failure @ Failure(error) =>
        val duration = (System.nanoTime() - startTime) / 1_000_000.0 // ms
        endSpan(
          ctx,
          success = false,
          attributes = attributes ++ Map(
            "duration_ms" -> JsonNumber(duration),
            "error"       -> JsonString(error.getMessage),
            "error_type"  -> JsonString(error.getClass.getName)
          )
        )
        failure
    }
  }

  /** Add an event to the current span
    * @param name
    *   The event name
    * @param attributes
    *   Optional key-value attributes for the event
    */
  def spanEvent(name: String, attributes: Map[String, Json] = Map.empty): Unit =
    {
      currentSpan.foreach { ctx =>
        val fields = Map(
          "event"     -> JsonString("span.event"),
          "trace_id"  -> JsonString(ctx.traceId),
          "span_id"   -> JsonString(ctx.spanId),
          "name"      -> JsonString(name),
          "timestamp" -> JsonNumber(System.currentTimeMillis().toDouble)
        ) ++ attributes

        logger.info(Json.obj(fields.toSeq*).toJsonString)
      }
    }
}
