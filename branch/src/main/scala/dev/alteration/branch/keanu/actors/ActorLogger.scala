package dev.alteration.branch.keanu.actors

import java.time.Instant

/** A logger for actor system events.
  *
  * This trait defines a simple logging interface that can be implemented to
  * integrate with different logging frameworks or custom logging solutions.
  */
trait ActorLogger {

  /** Log a debug message.
    *
    * @param message
    *   The message to log
    * @param actorRef
    *   Optional actor reference related to this message
    */
  def debug(message: String, actorRef: Option[ActorRefId] = None): Unit

  /** Log an info message.
    *
    * @param message
    *   The message to log
    * @param actorRef
    *   Optional actor reference related to this message
    */
  def info(message: String, actorRef: Option[ActorRefId] = None): Unit

  /** Log a warning message.
    *
    * @param message
    *   The message to log
    * @param actorRef
    *   Optional actor reference related to this message
    * @param error
    *   Optional throwable that caused the warning
    */
  def warn(
      message: String,
      actorRef: Option[ActorRefId] = None,
      error: Option[Throwable] = None
  ): Unit

  /** Log an error message.
    *
    * @param message
    *   The message to log
    * @param actorRef
    *   Optional actor reference related to this message
    * @param error
    *   Optional throwable that caused the error
    */
  def error(
      message: String,
      actorRef: Option[ActorRefId] = None,
      error: Option[Throwable] = None
  ): Unit

  /** Log an actor lifecycle event.
    *
    * @param event
    *   The lifecycle event to log
    * @param actorRef
    *   The actor reference this event relates to
    */
  def lifecycleEvent(event: ActorLifecycleEvent, actorRef: ActorRefId): Unit
}

/** Actor lifecycle events for logging.
  */
sealed trait ActorLifecycleEvent
case object ActorStarted                     extends ActorLifecycleEvent
case object ActorStopped                     extends ActorLifecycleEvent
case class ActorRestarted(reason: Throwable) extends ActorLifecycleEvent
case class ActorFailed(reason: Throwable)    extends ActorLifecycleEvent
case object ActorTerminated                  extends ActorLifecycleEvent

/** A no-op logger that discards all log messages.
  *
  * This is the default logger to avoid requiring users to set up logging if
  * they don't need it.
  */
object NoOpLogger extends ActorLogger {
  override def debug(message: String, actorRef: Option[ActorRefId]): Unit = ()
  override def info(message: String, actorRef: Option[ActorRefId]): Unit  = ()
  override def warn(
      message: String,
      actorRef: Option[ActorRefId],
      error: Option[Throwable]
  ): Unit = ()
  override def error(
      message: String,
      actorRef: Option[ActorRefId],
      error: Option[Throwable]
  ): Unit = ()
  override def lifecycleEvent(
      event: ActorLifecycleEvent,
      actorRef: ActorRefId
  ): Unit = ()
}

/** A simple console logger that writes to stdout/stderr.
  *
  * This is a basic implementation suitable for development and testing.
  */
class ConsoleLogger extends ActorLogger {
  private def timestamp(): String = Instant.now().toString

  private def formatMessage(
      level: String,
      message: String,
      actorRef: Option[ActorRefId]
  ): String = {
    val actorInfo = actorRef.map(ref => s" [${ref.toIdentifier}]").getOrElse("")
    s"${timestamp()} [$level]$actorInfo $message"
  }

  override def debug(message: String, actorRef: Option[ActorRefId]): Unit = {
    println(formatMessage("DEBUG", message, actorRef))
  }

  override def info(message: String, actorRef: Option[ActorRefId]): Unit = {
    println(formatMessage("INFO", message, actorRef))
  }

  override def warn(
      message: String,
      actorRef: Option[ActorRefId],
      error: Option[Throwable]
  ): Unit = {
    System.err.println(formatMessage("WARN", message, actorRef))
    error.foreach { e =>
      System.err.println(s"  Caused by: ${e.getClass.getName}: ${e.getMessage}")
    }
  }

  override def error(
      message: String,
      actorRef: Option[ActorRefId],
      error: Option[Throwable]
  ): Unit = {
    System.err.println(formatMessage("ERROR", message, actorRef))
    error.foreach { e =>
      System.err.println(s"  Caused by: ${e.getClass.getName}: ${e.getMessage}")
      if (e.getStackTrace.nonEmpty) {
        e.getStackTrace.take(3).foreach { frame =>
          System.err.println(s"    at $frame")
        }
      }
    }
  }

  override def lifecycleEvent(
      event: ActorLifecycleEvent,
      actorRef: ActorRefId
  ): Unit = {
    val message = event match {
      case ActorStarted           => "Actor started"
      case ActorStopped           => "Actor stopped"
      case ActorRestarted(reason) =>
        s"Actor restarted due to: ${reason.getClass.getSimpleName}"
      case ActorFailed(reason)    =>
        s"Actor failed: ${reason.getClass.getSimpleName}"
      case ActorTerminated        => "Actor terminated"
    }
    debug(message, Some(actorRef))
  }
}

/** A collecting logger that stores log messages in memory for testing.
  *
  * This logger is useful for verifying that correct log messages are being
  * generated during tests.
  */
class CollectingLogger extends ActorLogger {
  import scala.collection.mutable

  case class LogEntry(
      level: String,
      message: String,
      actorRef: Option[ActorRefId],
      error: Option[Throwable],
      timestamp: Instant
  )

  private val entries: mutable.ArrayBuffer[LogEntry] =
    mutable.ArrayBuffer.empty

  def getEntries: List[LogEntry] = entries.synchronized {
    entries.toList
  }

  def clear(): Unit = entries.synchronized {
    entries.clear()
  }

  override def debug(message: String, actorRef: Option[ActorRefId]): Unit = {
    entries.synchronized {
      entries += LogEntry("DEBUG", message, actorRef, None, Instant.now())
    }
  }

  override def info(message: String, actorRef: Option[ActorRefId]): Unit = {
    entries.synchronized {
      entries += LogEntry("INFO", message, actorRef, None, Instant.now())
    }
  }

  override def warn(
      message: String,
      actorRef: Option[ActorRefId],
      error: Option[Throwable]
  ): Unit = {
    entries.synchronized {
      entries += LogEntry("WARN", message, actorRef, error, Instant.now())
    }
  }

  override def error(
      message: String,
      actorRef: Option[ActorRefId],
      error: Option[Throwable]
  ): Unit = {
    entries.synchronized {
      entries += LogEntry("ERROR", message, actorRef, error, Instant.now())
    }
  }

  override def lifecycleEvent(
      event: ActorLifecycleEvent,
      actorRef: ActorRefId
  ): Unit = {
    val message = event match {
      case ActorStarted           => "Actor started"
      case ActorStopped           => "Actor stopped"
      case ActorRestarted(reason) =>
        s"Actor restarted due to: ${reason.getClass.getSimpleName}"
      case ActorFailed(reason)    =>
        s"Actor failed: ${reason.getClass.getSimpleName}"
      case ActorTerminated        => "Actor terminated"
    }
    debug(message, Some(actorRef))
  }
}
