package dev.alteration.branch.blammo

import dev.alteration.branch.friday.Json
import munit.FunSuite

import java.util.logging.{Level, LogRecord}

class JsonFormatterSpec extends FunSuite {

  test("JsonFormatter formats to Json") {
    val formatter            = JsonFormatter()
    val logRecord: LogRecord = new LogRecord(Level.INFO, "Test message!")

    assert(
      Json.parse(formatter.format(logRecord)).isRight
    )
  }

}
