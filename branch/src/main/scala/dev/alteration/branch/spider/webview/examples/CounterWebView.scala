package dev.alteration.branch.spider.webview.examples

import dev.alteration.branch.spider.webview.*

/** A simple counter WebView example.
  *
  * This demonstrates the basics of WebView:
  * - State management (count)
  * - Event handling (increment, decrement, reset)
  * - HTML rendering with event attributes
  */

// Define the state
case class CounterState(count: Int = 0)

// Define events using a sealed trait for type safety
sealed trait CounterEvent derives EventCodec
case object Increment extends CounterEvent
case object Decrement extends CounterEvent
case object Reset extends CounterEvent

// Define the WebView
class CounterWebView extends WebView[CounterState, CounterEvent] {

  override def mount(params: Map[String, String], session: Session): CounterState = {
    // Allow initial count to be set via URL params
    val initialCount = params.get("initial").flatMap(_.toIntOption).getOrElse(0)
    CounterState(count = initialCount)
  }

  override def handleEvent(event: CounterEvent, state: CounterState): CounterState = {
    event match {
      case Increment => state.copy(count = state.count + 1)
      case Decrement => state.copy(count = state.count - 1)
      case Reset => state.copy(count = 0)
    }
  }

  override def render(state: CounterState): String = {
    s"""
    <div id="counter" style="font-family: sans-serif; max-width: 400px; margin: 50px auto; padding: 30px; background: white; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
      <h1 style="margin: 0 0 30px 0; color: #2d3748; text-align: center;">Counter Example</h1>

      <div style="text-align: center; margin-bottom: 30px;">
        <div style="font-size: 4rem; font-weight: bold; color: #667eea; margin: 20px 0;">
          ${state.count}
        </div>
      </div>

      <div style="display: flex; gap: 10px; justify-content: center;">
        <button
          wv-click="Decrement"
          style="padding: 12px 24px; font-size: 1.2rem; background: #f56565; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: bold;">
          -
        </button>

        <button
          wv-click="Reset"
          style="padding: 12px 24px; font-size: 1rem; background: #718096; color: white; border: none; border-radius: 8px; cursor: pointer;">
          Reset
        </button>

        <button
          wv-click="Increment"
          style="padding: 12px 24px; font-size: 1.2rem; background: #48bb78; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: bold;">
          +
        </button>
      </div>

      <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #e2e8f0; text-align: center; color: #718096; font-size: 0.875rem;">
        <p style="margin: 0;">This is a Branch WebView example</p>
        <p style="margin: 5px 0 0 0;">State is managed on the server, updates happen in real-time</p>
      </div>
    </div>
    """
  }
}
