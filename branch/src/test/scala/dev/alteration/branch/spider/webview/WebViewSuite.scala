package dev.alteration.branch.spider.webview

import munit.FunSuite

/** Base trait for testing WebViews.
  *
  * Provides helper methods for testing WebView components without needing a
  * full server setup or WebSocket connections.
  *
  * Example:
  * {{{
  * class CounterWebViewTest extends WebViewSpec {
  *   test("counter increments") {
  *     val webView = new CounterWebView()
  *     val state = mount(webView, params = Map("initial" -> "5"))
  *
  *     assertEquals(state.count, 5)
  *
  *     val nextState = sendEvent(webView, Increment, state)
  *     assertEquals(nextState.count, 6)
  *
  *     val html = render(webView, nextState)
  *     assert(html.contains("6"))
  *   }
  * }
  * }}}
  */
trait WebViewSuite extends FunSuite {

  /** Mount a WebView with the given parameters and session.
    *
    * This calls the mount method on the WebView and returns the initial state.
    *
    * @param webView
    *   The WebView to mount
    * @param params
    *   URL query parameters (default: empty)
    * @param session
    *   Session data (default: empty session)
    * @tparam State
    *   The state type
    * @tparam Event
    *   The event type
    * @return
    *   The initial state
    */
  def mount[State, Event](
      webView: WebView[State, Event],
      params: Map[String, String] = Map.empty,
      session: Session = Session()
  ): State = {
    webView.mount(params, session)
  }

  /** Send an event to the WebView and get the new state.
    *
    * This calls handleEvent on the WebView with the given event and state.
    *
    * @param webView
    *   The WebView
    * @param event
    *   The event to send
    * @param state
    *   The current state
    * @tparam State
    *   The state type
    * @tparam Event
    *   The event type
    * @return
    *   The new state after processing the event
    */
  def sendEvent[State, Event](
      webView: WebView[State, Event],
      event: Event,
      state: State
  ): State = {
    webView.handleEvent(event, state)
  }

  /** Send an info message to the WebView and get the new state.
    *
    * This calls handleInfo on the WebView with the given message and state.
    *
    * @param webView
    *   The WebView
    * @param msg
    *   The info message to send
    * @param state
    *   The current state
    * @tparam State
    *   The state type
    * @tparam Event
    *   The event type
    * @return
    *   The new state after processing the message
    */
  def sendInfo[State, Event](
      webView: WebView[State, Event],
      msg: Any,
      state: State
  ): State = {
    webView.handleInfo(msg, state)
  }

  /** Render the WebView with the given state.
    *
    * This calls render on the WebView with the given state, applying the
    * beforeRender hook first.
    *
    * @param webView
    *   The WebView
    * @param state
    *   The state to render
    * @tparam State
    *   The state type
    * @tparam Event
    *   The event type
    * @return
    *   The rendered HTML string
    */
  def render[State, Event](
      webView: WebView[State, Event],
      state: State
  ): String = {
    val renderState = webView.beforeRender(state)
    webView.render(renderState)
  }

  /** Check if the rendered HTML contains a specific substring.
    *
    * @param html
    *   The HTML to check
    * @param substring
    *   The substring to look for
    * @return
    *   true if the HTML contains the substring
    */
  def htmlContains(html: String, substring: String): Boolean = {
    html.contains(substring)
  }

  /** Check if the rendered HTML matches a regular expression.
    *
    * @param html
    *   The HTML to check
    * @param pattern
    *   The regex pattern to match
    * @return
    *   true if the HTML matches the pattern
    */
  def htmlMatches(html: String, pattern: String): Boolean = {
    html.matches(pattern)
  }

  /** Assert that the HTML contains a specific substring.
    *
    * @param html
    *   The HTML to check
    * @param substring
    *   The substring that should be present
    * @param message
    *   Optional assertion message
    */
  def assertHtmlContains(
      html: String,
      substring: String,
      message: String = ""
  ): Unit = {
    val msg = if (message.isEmpty) {
      s"HTML should contain '$substring'"
    } else message

    assert(html.contains(substring), msg)
  }

  /** Assert that the HTML does not contain a specific substring.
    *
    * @param html
    *   The HTML to check
    * @param substring
    *   The substring that should not be present
    * @param message
    *   Optional assertion message
    */
  def assertHtmlDoesNotContain(
      html: String,
      substring: String,
      message: String = ""
  ): Unit = {
    val msg = if (message.isEmpty) {
      s"HTML should not contain '$substring'"
    } else message

    assert(!html.contains(substring), msg)
  }

  /** Assert that the HTML matches a regular expression.
    *
    * @param html
    *   The HTML to check
    * @param pattern
    *   The regex pattern to match
    * @param message
    *   Optional assertion message
    */
  def assertHtmlMatches(
      html: String,
      pattern: String,
      message: String = ""
  ): Unit = {
    val msg = if (message.isEmpty) {
      s"HTML should match pattern '$pattern'"
    } else message

    assert(html.matches(pattern), msg)
  }

  /** Helper to simulate a sequence of events and get the final state.
    *
    * This is useful for testing complex workflows with multiple events.
    *
    * @param webView
    *   The WebView
    * @param initialState
    *   The initial state
    * @param events
    *   The sequence of events to send
    * @tparam State
    *   The state type
    * @tparam Event
    *   The event type
    * @return
    *   The final state after all events
    */
  def sendEvents[State, Event](
      webView: WebView[State, Event],
      initialState: State,
      events: Seq[Event]
  ): State = {
    events.foldLeft(initialState) { (state, event) =>
      sendEvent(webView, event, state)
    }
  }

  /** Helper to mount, send events, and render in one step.
    *
    * This is useful for simple test cases.
    *
    * @param webView
    *   The WebView
    * @param events
    *   The events to send
    * @param params
    *   URL query parameters (default: empty)
    * @param session
    *   Session data (default: empty session)
    * @tparam State
    *   The state type
    * @tparam Event
    *   The event type
    * @return
    *   A tuple of (final state, rendered HTML)
    */
  def mountAndRender[State, Event](
      webView: WebView[State, Event],
      events: Seq[Event] = Seq.empty,
      params: Map[String, String] = Map.empty,
      session: Session = Session()
  ): (State, String) = {
    val initialState = mount(webView, params, session)
    val finalState   = sendEvents(webView, initialState, events)
    val html         = render(webView, finalState)
    (finalState, html)
  }
}
