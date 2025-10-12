package dev.alteration.branch.spider.webview

import dev.alteration.branch.testkit.fixtures.WebViewSuite

import scala.annotation.nowarn

/** Tests for WebView lifecycle and hooks.
  *
  * Tests the core WebView trait functionality including mount, handleEvent,
  * render, and various lifecycle hooks.
  */
class WebViewLifecycleSuite extends WebViewSuite {

  // === Test State and Events ===

  case class CounterState(count: Int)

  sealed trait CounterEvent derives EventCodec
  case object Increment           extends CounterEvent
  case object Decrement           extends CounterEvent
  case class SetCount(value: Int) extends CounterEvent

  // === Basic WebView Implementations ===

  class SimpleCounterView extends WebView[CounterState, CounterEvent] {
    def mount(params: Map[String, String], session: Session): CounterState = {
      val initial = params.get("initial").map(_.toInt).getOrElse(0)
      CounterState(initial)
    }

    def handleEvent(event: CounterEvent, state: CounterState): CounterState = {
      event match {
        case Increment   => state.copy(count = state.count + 1)
        case Decrement   => state.copy(count = state.count - 1)
        case SetCount(n) => state.copy(count = n)
      }
    }

    def render(state: CounterState): String = {
      s"<div>Count: ${state.count}</div>"
    }
  }

  // === Basic Lifecycle Tests ===

  test("mount initializes state with default value") {
    val view  = new SimpleCounterView()
    val state = mount(view)

    assertEquals(state.count, 0)
  }

  test("mount initializes state with params") {
    val view  = new SimpleCounterView()
    val state = mount(view, params = Map("initial" -> "42"))

    assertEquals(state.count, 42)
  }

  test("handleEvent Increment increases count") {
    val view     = new SimpleCounterView()
    val state    = mount(view)
    val newState = sendEvent(view, Increment, state)

    assertEquals(newState.count, 1)
  }

  test("handleEvent Decrement decreases count") {
    val view     = new SimpleCounterView()
    val state    = mount(view, params = Map("initial" -> "5"))
    val newState = sendEvent(view, Decrement, state)

    assertEquals(newState.count, 4)
  }

  test("handleEvent SetCount sets specific value") {
    val view     = new SimpleCounterView()
    val state    = mount(view)
    val newState = sendEvent(view, SetCount(100), state)

    assertEquals(newState.count, 100)
  }

  test("render generates HTML from state") {
    val view  = new SimpleCounterView()
    val state = CounterState(42)
    val html  = render(view, state)

    assertEquals(html, "<div>Count: 42</div>")
  }

  test("multiple events in sequence") {
    val view       = new SimpleCounterView()
    val state      = mount(view)
    val finalState = sendEvents(
      view,
      state,
      Seq(
        Increment,
        Increment,
        Increment,
        Decrement,
        SetCount(10)
      )
    )

    assertEquals(finalState.count, 10)
  }

  test("mountAndRender helper") {
    val view          = new SimpleCounterView()
    val (state, html) = mountAndRender(
      view,
      events = Seq(Increment, Increment),
      params = Map("initial" -> "5")
    )

    assertEquals(state.count, 7)
    assertEquals(html, "<div>Count: 7</div>")
  }

  // === Lifecycle Hooks Tests ===

  test("beforeRender hook can transform state") {
    class TransformingView extends WebView[CounterState, CounterEvent] {
      def mount(params: Map[String, String], session: Session): CounterState =
        CounterState(0)

      def handleEvent(event: CounterEvent, state: CounterState): CounterState =
        state

      override def beforeRender(state: CounterState): CounterState = {
        // Double the count for rendering
        state.copy(count = state.count * 2)
      }

      def render(state: CounterState): String =
        s"<div>${state.count}</div>"
    }

    val view  = new TransformingView()
    val state = CounterState(5)
    val html  = render(view, state)

    // Original state unchanged
    assertEquals(state.count, 5)
    // Rendered with doubled value
    assertEquals(html, "<div>10</div>")
  }

  test("handleInfo default implementation returns unchanged state") {
    val view     = new SimpleCounterView()
    val state    = CounterState(42)
    val newState = sendInfo(view, "some-message", state)

    assertEquals(newState, state)
  }

  test("handleInfo can be overridden to process messages") {
    class MessageHandlingView extends WebView[CounterState, CounterEvent] {
      def mount(params: Map[String, String], session: Session): CounterState =
        CounterState(0)

      def handleEvent(event: CounterEvent, state: CounterState): CounterState =
        state

      override def handleInfo(msg: Any, state: CounterState): CounterState = {
        msg match {
          case "reset" => CounterState(0)
          case n: Int  => state.copy(count = n)
          case _       => state
        }
      }

      def render(state: CounterState): String =
        s"<div>${state.count}</div>"
    }

    val view  = new MessageHandlingView()
    val state = CounterState(42)

    val resetState = sendInfo(view, "reset", state)
    assertEquals(resetState.count, 0)

    val updatedState = sendInfo(view, 100, state)
    assertEquals(updatedState.count, 100)

    val unchangedState = sendInfo(view, "unknown", state)
    assertEquals(unchangedState, state)
  }

  // === Error Handling Tests ===

  test("onError default implementation returns None") {
    val view   = new SimpleCounterView()
    val state  = CounterState(42)
    val error  = new RuntimeException("Test error")
    val result = view.onError(error, state, ErrorPhase.Event)

    assertEquals(result, None)
  }

  test("onError can be overridden to recover from errors") {
    class ErrorRecoveringView extends WebView[CounterState, CounterEvent] {
      def mount(params: Map[String, String], session: Session): CounterState =
        CounterState(0)

      def handleEvent(event: CounterEvent, state: CounterState): CounterState =
        state

      override def onError(
          error: Throwable,
          state: CounterState,
          phase: ErrorPhase
      ): Option[CounterState] = {
        // Reset to safe state on error
        Some(CounterState(0))
      }

      def render(state: CounterState): String =
        s"<div>${state.count}</div>"
    }

    val view   = new ErrorRecoveringView()
    val state  = CounterState(42)
    val error  = new RuntimeException("Test error")
    val result = view.onError(error, state, ErrorPhase.Render)

    assertEquals(result, Some(CounterState(0)))
  }

  test("renderError generates error HTML") {
    val view  = new SimpleCounterView()
    val error = new RuntimeException("Something went wrong")
    val html  = view.renderError(error, ErrorPhase.Event)

    assert(html.contains("Error in Event Handling"))
    assert(html.contains("Something went wrong"))
    assert(html.contains("Reload Page"))
  }

  test("renderError handles null error message") {
    val view  = new SimpleCounterView()
    val error = new RuntimeException()
    val html  = view.renderError(error, ErrorPhase.Mount)

    assert(html.contains("RuntimeException"))
  }

  test("shouldRetry default implementation returns false") {
    val view   = new SimpleCounterView()
    val error  = new RuntimeException("Test error")
    val result = view.shouldRetry(error, ErrorPhase.Event, attemptCount = 0)

    assertEquals(result, false)
  }

  test("shouldRetry can be overridden for custom retry logic") {
    class RetryingView extends WebView[CounterState, CounterEvent] {
      def mount(params: Map[String, String], session: Session): CounterState =
        CounterState(0)

      def handleEvent(event: CounterEvent, state: CounterState): CounterState =
        state

      override def shouldRetry(
          error: Throwable,
          phase: ErrorPhase,
          attemptCount: Int
      ): Boolean = {
        // Retry up to 3 times
        attemptCount < 3
      }

      def render(state: CounterState): String =
        s"<div>${state.count}</div>"
    }

    val view  = new RetryingView()
    val error = new RuntimeException("Test error")

    assertEquals(view.shouldRetry(error, ErrorPhase.Event, 0), true)
    assertEquals(view.shouldRetry(error, ErrorPhase.Event, 1), true)
    assertEquals(view.shouldRetry(error, ErrorPhase.Event, 2), true)
    assertEquals(view.shouldRetry(error, ErrorPhase.Event, 3), false)
  }

  // === Session Tests ===

  test("mount receives session data") {
    class SessionAwareView extends WebView[CounterState, CounterEvent] {
      def mount(params: Map[String, String], session: Session): CounterState = {
        val userCount = session.get[Int]("userCount").getOrElse(0)
        CounterState(userCount)
      }

      def handleEvent(event: CounterEvent, state: CounterState): CounterState =
        state

      def render(state: CounterState): String =
        s"<div>${state.count}</div>"
    }

    val view    = new SessionAwareView()
    val session = Session(Map("userCount" -> 42))
    val state   = mount(view, session = session)

    assertEquals(state.count, 42)
  }

  // === ErrorPhase Tests ===

  test("ErrorPhase enum values") {
    assertEquals(ErrorPhase.Mount.name, "Mount")
    assertEquals(ErrorPhase.Event.name, "Event Handling")
    assertEquals(ErrorPhase.Info.name, "Info Handling")
    assertEquals(ErrorPhase.Render.name, "Rendering")
    assertEquals(ErrorPhase.Lifecycle.name, "Lifecycle Hook")
  }

  // === Complex State Scenarios ===

  test("WebView with complex state") {
    case class TodoState(
        todos: List[String],
        filter: String,
        input: String
    )

    @nowarn
    sealed trait TodoEvent derives EventCodec
    case class AddTodo(text: String)     extends TodoEvent
    case class RemoveTodo(index: Int)    extends TodoEvent
    case class SetFilter(filter: String) extends TodoEvent

    class TodoView extends WebView[TodoState, TodoEvent] {
      def mount(params: Map[String, String], session: Session): TodoState = {
        TodoState(Nil, "all", "")
      }

      def handleEvent(event: TodoEvent, state: TodoState): TodoState = {
        event match {
          case AddTodo(text)     =>
            state.copy(todos = state.todos :+ text)
          case RemoveTodo(index) =>
            state.copy(todos = state.todos.patch(index, Nil, 1))
          case SetFilter(filter) =>
            state.copy(filter = filter)
        }
      }

      def render(state: TodoState): String = {
        s"<div>Todos: ${state.todos.mkString(", ")}</div>"
      }
    }

    val view      = new TodoView()
    val state     = mount(view)
    val withTodos = sendEvents(
      view,
      state,
      Seq(
        AddTodo("Buy milk"),
        AddTodo("Walk dog"),
        AddTodo("Write tests")
      )
    )

    assertEquals(withTodos.todos.length, 3)
    assertEquals(withTodos.todos(0), "Buy milk")

    val afterRemoval = sendEvent(view, RemoveTodo(1), withTodos)
    assertEquals(afterRemoval.todos.length, 2)
    assertEquals(afterRemoval.todos, List("Buy milk", "Write tests"))
  }

  test("WebView state immutability") {
    val view   = new SimpleCounterView()
    val state1 = CounterState(0)
    val state2 = sendEvent(view, Increment, state1)
    val state3 = sendEvent(view, Increment, state2)

    // Each state should be independent
    assertEquals(state1.count, 0)
    assertEquals(state2.count, 1)
    assertEquals(state3.count, 2)
  }

  test("WebView with no initial params") {
    val view  = new SimpleCounterView()
    val state = mount(view, params = Map.empty)

    assertEquals(state.count, 0)
  }

  test("WebView handles empty events sequence") {
    val view       = new SimpleCounterView()
    val state      = mount(view)
    val finalState = sendEvents(view, state, Seq.empty)

    assertEquals(finalState, state)
  }
}
