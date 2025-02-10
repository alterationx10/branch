package dev.wishingtree.branch.testkit.fixtures

import java.util.logging.{Handler, Logger, LogRecord}

trait LoggerFixtureSuite extends munit.FunSuite {

  case class TestLogHandler() extends Handler {

    val records: collection.mutable.ArrayBuffer[LogRecord] =
      collection.mutable.ArrayBuffer.empty

    override def publish(record: LogRecord): Unit =
      records += record

    override def flush(): Unit = ()

    override def close(): Unit = ()
  }

  def loggerFixture: FunFixture[(Logger, TestLogHandler)] =
    FunFixture[(Logger, TestLogHandler)](
      setup = { test =>
        val handler = TestLogHandler()
        val logger  = Logger.getLogger(test.name)
        logger.setUseParentHandlers(false)
        logger.getHandlers.foreach(logger.removeHandler)
        logger.setLevel(java.util.logging.Level.ALL)
        logger.addHandler(handler)
        (logger, handler)
      },
      teardown = { (_, _) =>
        ()
      }
    )

}
