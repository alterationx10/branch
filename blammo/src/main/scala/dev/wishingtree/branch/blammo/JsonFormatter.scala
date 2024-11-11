package dev.wishingtree.branch.blammo

import dev.wishingtree.branch.friday.Json
import dev.wishingtree.branch.friday.Json.*

import java.util.logging.{Formatter, LogRecord}

case class JsonFormatter() extends Formatter {

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
      .toJsonString
  }
}
