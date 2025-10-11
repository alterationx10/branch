package dev.alteration.branch.spider.webview

/** Example state for a counter WebView */
case class CounterState(count: Int, name: String)

/** A simple counter WebView demonstrating the Branch WebView framework.
  *
  * This is a proof-of-concept that shows:
  *   - State management (count and name)
  *   - Event handling (increment, decrement, name changes)
  *   - HTML rendering with WebView attributes
  *
  * Example usage:
  * {{{
  * val actorSystem = ActorSystem()
  * val handler = WebViewHandler(actorSystem, () => CounterWebView())
  *
  * val server = WebSocketServer(8080)
  * server.addRoute("/counter", handler)
  * }}}
  */
class CounterWebView extends WebView[CounterState] {

  override def mount(
      params: Map[String, String],
      session: Session
  ): CounterState = {
    // Initialize count from URL params or default to 0
    val initialCount = params.get("count").flatMap(_.toIntOption).getOrElse(0)
    val initialName  = params.getOrElse("name", "World")

    CounterState(count = initialCount, name = initialName)
  }

  override def handleEvent(
      event: String,
      payload: Map[String, Any],
      state: CounterState
  ): CounterState = {
    val newState = event match {
      case "increment" =>
        state.copy(count = state.count + 1)

      case "decrement" =>
        state.copy(count = state.count - 1)

      case "reset" =>
        state.copy(count = 0)

      case "update-name" =>
        val newName = payload.get("value").map(_.toString).getOrElse(state.name)
        state.copy(name = newName)

      case _ =>
        // Unknown event, don't change state
        state
    }
    println(s"$state => $newState")
    newState
  }

  override def render(state: CounterState): String = {
    s"""
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 50px auto; padding: 20px; border: 1px solid #ccc; border-radius: 8px;">
        <h1 style="text-align: center; color: #333;">Branch WebView Counter</h1>

        <div style="text-align: center; margin: 30px 0;">
          <h2>Hello, ${escapeHtml(state.name)}!</h2>
        </div>

        <div style="text-align: center; margin: 30px 0;">
          <div style="font-size: 48px; font-weight: bold; color: ${getCountColor(state.count)};">
            ${state.count}
          </div>
        </div>

        <div style="display: flex; gap: 10px; justify-content: center; margin: 20px 0;">
          <button
            wv-click="decrement"
            style="padding: 10px 20px; font-size: 18px; cursor: pointer; background: #f44336; color: white; border: none; border-radius: 4px;">
            -
          </button>

          <button
            wv-click="reset"
            style="padding: 10px 20px; font-size: 18px; cursor: pointer; background: #9e9e9e; color: white; border: none; border-radius: 4px;">
            Reset
          </button>

          <button
            wv-click="increment"
            style="padding: 10px 20px; font-size: 18px; cursor: pointer; background: #4caf50; color: white; border: none; border-radius: 4px;">
            +
          </button>
        </div>

        <div style="margin: 30px 0;">
          <label style="display: block; margin-bottom: 8px; font-weight: bold;">
            Your Name:
          </label>
          <input
            type="text"
            wv-change="update-name"
            value="${escapeHtml(state.name)}"
            style="width: 100%; padding: 10px; font-size: 16px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box;"
          />
        </div>

        <div style="text-align: center; margin-top: 40px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 14px;">
          <p>This is a real-time, stateful UI powered by Branch WebView</p>
          <p>State lives on the server, updates happen over WebSocket</p>
        </div>
      </div>
    """
  }

  /** Get color based on count value */
  private def getCountColor(count: Int): String = {
    if count > 0 then "#4caf50"      // Green for positive
    else if count < 0 then "#f44336" // Red for negative
    else "#333"                      // Dark gray for zero
  }

  /** Escape HTML special characters to prevent XSS */
  private def escapeHtml(text: String): String = {
    text
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")
  }

  override def terminate(reason: Option[Throwable], state: CounterState): Unit = {
    // Cleanup if needed
    println(s"CounterWebView terminated. Final count: ${state.count}")
  }
}
