package dev.alteration.branch.spider.webview.devtools

import java.time.Instant

/** DevTools for Branch WebView - provides debugging and monitoring capabilities.
  *
  * Features:
  * - State timeline inspector
  * - Event log viewer
  * - Performance metrics
  * - WebSocket connection status
  */

/** A single entry in the DevTools timeline.
  *
  * @param timestamp
  *   When this entry was recorded
  * @param eventType
  *   The type of event (StateChange, Event, Mount, etc.)
  * @param data
  *   Associated data (state snapshot, event details, etc.)
  */
case class TimelineEntry(
    timestamp: Instant,
    eventType: String,
    data: Map[String, Any]
)

/** DevTools state tracking for a single WebView instance.
  *
  * @param viewId
  *   Unique identifier for this WebView instance
  * @param timeline
  *   History of state changes and events
  * @param connectionStatus
  *   Current WebSocket connection status
  * @param metrics
  *   Performance metrics
  */
case class DevToolsState(
    viewId: String,
    timeline: List[TimelineEntry] = List.empty,
    connectionStatus: ConnectionStatus = ConnectionStatus.Connected,
    metrics: PerformanceMetrics = PerformanceMetrics()
) {

  /** Add a timeline entry for mounting. */
  def recordMount(state: Any): DevToolsState = {
    val entry = TimelineEntry(
      timestamp = Instant.now(),
      eventType = "Mount",
      data = Map(
        "state" -> state.toString
      )
    )
    copy(timeline = timeline :+ entry)
  }

  /** Add a timeline entry for an event. */
  def recordEvent(event: Any, oldState: Any, newState: Any, renderTimeMs: Long): DevToolsState = {
    val entry = TimelineEntry(
      timestamp = Instant.now(),
      eventType = "Event",
      data = Map(
        "event" -> event.toString,
        "oldState" -> oldState.toString,
        "newState" -> newState.toString,
        "renderTimeMs" -> renderTimeMs
      )
    )
    copy(
      timeline = timeline :+ entry,
      metrics = metrics.recordEvent(renderTimeMs)
    )
  }

  /** Add a timeline entry for an info message. */
  def recordInfo(msg: Any, oldState: Any, newState: Any): DevToolsState = {
    val entry = TimelineEntry(
      timestamp = Instant.now(),
      eventType = "Info",
      data = Map(
        "message" -> msg.toString,
        "oldState" -> oldState.toString,
        "newState" -> newState.toString
      )
    )
    copy(timeline = timeline :+ entry)
  }

  /** Update connection status. */
  def updateConnectionStatus(status: ConnectionStatus): DevToolsState = {
    copy(connectionStatus = status)
  }

  /** Get the most recent timeline entries (useful for limiting display). */
  def recentTimeline(limit: Int = 50): List[TimelineEntry] = {
    timeline.takeRight(limit)
  }
}

/** WebSocket connection status. */
enum ConnectionStatus {
  case Connected
  case Disconnected
  case Error(message: String)
}

/** Performance metrics for a WebView. */
case class PerformanceMetrics(
    totalEvents: Long = 0,
    totalRenderTimeMs: Long = 0,
    minRenderTimeMs: Long = Long.MaxValue,
    maxRenderTimeMs: Long = 0
) {

  /** Record a new event and its render time. */
  def recordEvent(renderTimeMs: Long): PerformanceMetrics = {
    copy(
      totalEvents = totalEvents + 1,
      totalRenderTimeMs = totalRenderTimeMs + renderTimeMs,
      minRenderTimeMs = math.min(minRenderTimeMs, renderTimeMs),
      maxRenderTimeMs = math.max(maxRenderTimeMs, renderTimeMs)
    )
  }

  /** Calculate average render time. */
  def avgRenderTimeMs: Double = {
    if (totalEvents == 0) 0.0
    else totalRenderTimeMs.toDouble / totalEvents.toDouble
  }
}
