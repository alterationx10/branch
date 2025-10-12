package dev.alteration.branch.spider.webview.html

import Attr.*

import scala.annotation.nowarn
import scala.reflect.ClassTag

/** WebView-specific HTML attributes.
  *
  * These attributes enable reactive, server-side event handling in Branch
  * WebView. When a user interacts with an element that has a WebView attribute,
  * an event is sent to the server over WebSocket.
  *
  * Example with string events (backward compatible):
  * {{{
  * button(wvClick := "increment")("Click me")
  * input(wvChange := "update-name", value := name)
  * form(wvSubmit := "save")
  * }}}
  *
  * Example with typed events:
  * {{{
  * sealed trait MyEvent
  * case object Increment extends MyEvent
  * case class SetCount(n: Int) extends MyEvent
  *
  * button(wvClick := Increment)("Click me")
  * button(wvClick := SetCount(42))("Set to 42")
  * }}}
  *
  * The client-side JavaScript listens for these attributes and automatically
  * wires up event handlers that send events to the server.
  */
object WebViewAttributes {

  /** Attribute builder for WebView events.
    *
    * @param eventType
    *   The type of event (e.g., "click", "change", "submit")
    */
  case class WebViewAttrBuilder(eventType: String) {

    /** Create a WebView event attribute from a string event name.
      *
      * @param eventName
      *   The name of the event to send to the server
      */
    def :=(eventName: String): Attr = {
      StringAttr(s"wv-$eventType", eventName)
    }

    /** Create a WebView event attribute from a typed event.
      *
      * This extracts the event type name and uses it as the event identifier.
      * For case objects, it uses the simple class name. For case classes, it
      * uses the class name (parameters are sent separately).
      *
      * @param event
      *   The typed event to send to the server
      */
    @nowarn
    def :=[Event](event: Event)(using ct: ClassTag[Event]): Attr = {
      // Extract the simple name of the event type
      // For case objects: "Increment" -> "Increment"
      // For case classes: "SetCount" -> "SetCount"
      val eventName = ct.runtimeClass.getSimpleName.stripSuffix("$")
      StringAttr(s"wv-$eventType", eventName)
    }
  }

  // === Primary WebView Attributes ===

  /** Handle click events.
    *
    * Fires when the user clicks the element.
    *
    * {{{
    * button(wvClick := "increment")("+")
    * // When clicked, sends: { event: "increment", payload: { target: elementId } }
    * }}}
    */
  val wvClick = WebViewAttrBuilder("click")

  /** Handle change events (input, select, textarea).
    *
    * Fires when the value of a form field changes.
    *
    * {{{
    * input(wvChange := "update-name")
    * // On change, sends: { event: "update-name", payload: { value: inputValue } }
    * }}}
    */
  val wvChange = WebViewAttrBuilder("change")

  /** Handle form submit events.
    *
    * Fires when a form is submitted. Automatically prevents default form
    * submission.
    *
    * {{{
    * form(wvSubmit := "save")(...)
    * // On submit, sends: { event: "save", payload: { formData: {...} } }
    * }}}
    */
  val wvSubmit = WebViewAttrBuilder("submit")

  /** Handle input events (fires on every keystroke).
    *
    * Similar to wvChange but fires immediately on each input.
    *
    * {{{
    * input(wvInput := "search")
    * // On each keystroke, sends: { event: "search", payload: { value: inputValue } }
    * }}}
    */
  val wvInput = WebViewAttrBuilder("input")

  /** Handle focus events.
    *
    * Fires when an element receives focus.
    *
    * {{{
    * input(wvFocus := "field-focused")
    * }}}
    */
  val wvFocus = WebViewAttrBuilder("focus")

  /** Handle blur events.
    *
    * Fires when an element loses focus.
    *
    * {{{
    * input(wvBlur := "field-blurred")
    * }}}
    */
  val wvBlur = WebViewAttrBuilder("blur")

  /** Handle keydown events.
    *
    * Fires when a key is pressed.
    *
    * {{{
    * input(wvKeydown := "handle-keypress")
    * // Sends: { event: "handle-keypress", payload: { key: "Enter", ... } }
    * }}}
    */
  val wvKeydown = WebViewAttrBuilder("keydown")

  /** Handle keyup events.
    *
    * Fires when a key is released.
    *
    * {{{
    * input(wvKeyup := "key-released")
    * }}}
    */
  val wvKeyup = WebViewAttrBuilder("keyup")

  /** Handle mouseenter events.
    *
    * Fires when the mouse enters an element.
    *
    * {{{
    * div(wvMouseenter := "show-tooltip")
    * }}}
    */
  val wvMouseenter = WebViewAttrBuilder("mouseenter")

  /** Handle mouseleave events.
    *
    * Fires when the mouse leaves an element.
    *
    * {{{
    * div(wvMouseleave := "hide-tooltip")
    * }}}
    */
  val wvMouseleave = WebViewAttrBuilder("mouseleave")

  // === Advanced WebView Attributes ===

  /** Debounce events (useful for search inputs).
    *
    * Delays sending events until the user stops typing.
    *
    * {{{
    * input(
    *   wvInput := "search",
    *   wvDebounce := "300"  // Wait 300ms after last keystroke
    * )
    * }}}
    */
  val wvDebounce = Attributes.AttrBuilder("wv-debounce")

  /** Throttle events (rate-limit event sending).
    *
    * Limits how often events can be sent.
    *
    * {{{
    * div(
    *   wvMouseenter := "track-hover",
    *   wvThrottle := "1000"  // Max once per second
    * )
    * }}}
    */
  val wvThrottle = Attributes.AttrBuilder("wv-throttle")

  /** Attach a value to the event payload.
    *
    * Useful for passing additional context with the event.
    *
    * {{{
    * button(
    *   wvClick := "select-item",
    *   wvValue := todo.id
    * )("Select")
    * // Sends: { event: "select-item", payload: { value: todo.id } }
    * }}}
    */
  val wvValue = Attributes.AttrBuilder("wv-value")

  /** Target element for event context.
    *
    * Specifies which element's data should be sent with the event.
    *
    * {{{
    * button(
    *   wvClick := "delete-item",
    *   wvTarget := item.id
    * )("Delete")
    * // Sends: { event: "delete-item", payload: { target: item.id } }
    * }}}
    */
  val wvTarget = Attributes.AttrBuilder("wv-target")

  /** Disable updates for this element.
    *
    * Prevents the WebView from updating this element's content on re-render.
    * Useful for preserving input focus or scroll position.
    *
    * {{{
    * input(
    *   wvChange := "update",
    *   wvIgnore := true
    * )
    * }}}
    */
  val wvIgnore = Attributes.AttrBuilder("wv-ignore")

  // === Convenience Helpers ===

  /** Create a custom WebView attribute.
    *
    * {{{
    * div(wvAttr("custom-event") := "handle-custom")
    * }}}
    */
  def wvAttr(eventType: String): WebViewAttrBuilder =
    WebViewAttrBuilder(eventType)

  /** Create a WebView click handler with a target value.
    *
    * This is a common pattern for list items, buttons with IDs, etc.
    *
    * {{{
    * button(wvClickTarget("delete-todo", todo.id))("Delete")
    * // Equivalent to:
    * // button(wvClick := "delete-todo", wvTarget := todo.id)
    * }}}
    */
  def wvClickTarget(eventName: String, targetValue: String): List[Attr] = {
    List(
      wvClick  := eventName,
      wvTarget := targetValue
    )
  }

  /** Create a WebView click handler with a custom value.
    *
    * {{{
    * button(wvClickValue("set-filter", "active"))("Active")
    * // Equivalent to:
    * // button(wvClick := "set-filter", wvValue := "active")
    * }}}
    */
  def wvClickValue(eventName: String, value: String): List[Attr] = {
    List(
      wvClick := eventName,
      wvValue := value
    )
  }

  /** Create a debounced input handler.
    *
    * {{{
    * input(wvDebounceInput("search", 300))
    * // Equivalent to:
    * // input(wvInput := "search", wvDebounce := "300")
    * }}}
    */
  def wvDebounceInput(eventName: String, delayMs: Int): List[Attr] = {
    List(
      wvInput    := eventName,
      wvDebounce := delayMs.toString
    )
  }

  /** Create a throttled event handler.
    *
    * {{{
    * div(wvThrottleClick("track-click", 1000))
    * // Equivalent to:
    * // div(wvClick := "track-click", wvThrottle := "1000")
    * }}}
    */
  def wvThrottleClick(eventName: String, intervalMs: Int): List[Attr] = {
    List(
      wvClick    := eventName,
      wvThrottle := intervalMs.toString
    )
  }
}
