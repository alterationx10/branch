package dev.wishingtree.branch.blammo

import dev.wishingtree.branch.friday.Json
import dev.wishingtree.branch.friday.Json.*

import java.util.logging.{Formatter, LogRecord}

/** A formatter for logging records as JSON
  */
case class JsonFormatter() extends Formatter {

  /** Formats a log record to JSON. If the message can be parsed as JSON, it
    * will be included as a separate jsonMessage field.
    */
  override def format(record: LogRecord): String = {
    Json
      .obj(
        "name"        -> jsonOrNull(record.getLoggerName),
        "level"       -> JsonString(record.getLevel.toString),
        "time"        -> JsonString(record.getInstant.toString),
        "message"     -> jsonOrNull(record.getMessage),
        "jsonMessage" -> Json.parse(record.getMessage).getOrElse(JsonNull),
        "error"       -> Json.throwable(record.getThrown)
      )
      .toJsonString + System.lineSeparator()
  }
}
