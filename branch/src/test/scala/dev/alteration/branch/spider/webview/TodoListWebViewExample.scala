package dev.alteration.branch.spider.webview

import dev.alteration.branch.spider.webview.html.Html
import dev.alteration.branch.spider.webview.html.Tags.*
import dev.alteration.branch.spider.webview.html.Attributes.*
import dev.alteration.branch.spider.webview.html.Components.*
import dev.alteration.branch.spider.webview.html.MustachioHelpers.*

/** Example TodoList WebView demonstrating Phase 4b features:
  *   - Typed events (sealed trait with EventCodec)
  *   - Lifecycle hooks (afterMount for subscriptions)
  *   - HTML DSL with components
  *   - Mustachio template integration
  *   - Testing with WebViewSpec
  */

// ===== State =====

case class TodoItem(
    id: Int,
    text: String,
    completed: Boolean
)

case class TodoListState(
    items: List[TodoItem],
    filter: TodoFilter,
    nextId: Int
)

sealed trait TodoFilter derives EventCodec
case object ShowAll extends TodoFilter
case object ShowActive extends TodoFilter
case object ShowCompleted extends TodoFilter

// ===== Events (Typed!) =====

sealed trait TodoEvent derives EventCodec
case class AddTodo(text: String) extends TodoEvent
case class ToggleTodo(id: Int) extends TodoEvent
case class DeleteTodo(id: Int) extends TodoEvent
case class SetFilter(filter: TodoFilter) extends TodoEvent
case object ClearCompleted extends TodoEvent

// ===== WebView Implementation =====

class TodoListWebView extends WebView[TodoListState, TodoEvent] {

  override def mount(params: Map[String, String], session: Session): TodoListState = {
    TodoListState(
      items = List(
        TodoItem(1, "Learn Branch WebView", completed = false),
        TodoItem(2, "Build awesome apps", completed = false)
      ),
      filter = ShowAll,
      nextId = 3
    )
  }

  // ===== Lifecycle Hook: afterMount =====
  override def afterMount(state: TodoListState, context: WebViewContext): Unit = {
    // Example: Subscribe to pub/sub, schedule tasks, etc.
    // context.sendSelf(RefreshMessage)  // Can send messages to self
    // context.tellPath("/user/other-actor", SomeMessage)  // Can talk to other actors
    ()
  }

  // ===== Lifecycle Hook: beforeUpdate =====
  override def beforeUpdate(event: TodoEvent, state: TodoListState, context: WebViewContext): Unit = {
    // Example: Log events, validate, perform authorization checks
    println(s"Processing event: $event")
  }

  // ===== Lifecycle Hook: afterUpdate =====
  override def afterUpdate(event: TodoEvent, oldState: TodoListState, newState: TodoListState, context: WebViewContext): Unit = {
    // Example: Trigger side effects based on state changes
    if (oldState.items.length != newState.items.length) {
      println(s"Todo count changed: ${oldState.items.length} -> ${newState.items.length}")
    }
  }

  // ===== Lifecycle Hook: beforeRender =====
  override def beforeRender(state: TodoListState): TodoListState = {
    // Example: Add computed fields for rendering
    // In this case, we don't need to transform state
    state
  }

  // ===== Event Handler (with exhaustiveness checking!) =====
  override def handleEvent(event: TodoEvent, state: TodoListState): TodoListState = event match {
    case AddTodo(text) if text.trim.nonEmpty =>
      val newItem = TodoItem(state.nextId, text.trim, completed = false)
      state.copy(
        items = state.items :+ newItem,
        nextId = state.nextId + 1
      )

    case AddTodo(_) =>
      // Empty text, ignore
      state

    case ToggleTodo(id) =>
      state.copy(
        items = state.items.map { item =>
          if (item.id == id) item.copy(completed = !item.completed)
          else item
        }
      )

    case DeleteTodo(id) =>
      state.copy(items = state.items.filterNot(_.id == id))

    case SetFilter(filter) =>
      state.copy(filter = filter)

    case ClearCompleted =>
      state.copy(items = state.items.filterNot(_.completed))
  }

  // ===== Render (using HTML DSL + Mustachio) =====
  override def render(state: TodoListState): String = {
    val filteredItems = filterItems(state.items, state.filter)
    val activeCount = state.items.count(!_.completed)
    val completedCount = state.items.count(_.completed)

    div(cls := "container", style := "max-width: 600px; margin: 20px auto")(
      // Header with Mustachio template
      mustache(
        """
        <header style="text-align: center; margin-bottom: 20px;">
          <h1 style="color: #667eea; font-size: 2.5rem;">Todo List</h1>
          <p style="color: #718096;">{{active}} active, {{completed}} completed</p>
        </header>
        """,
        Map(
          "active" -> activeCount,
          "completed" -> completedCount
        )
      ),

      // Add todo form
      div(cls := "add-todo", style := "margin-bottom: 20px;")(
        textInput(
          "new-todo",
          "",
          "add-todo",
          placeholder = Some("What needs to be done?"),
          extraAttrs = Seq(
            id := "new-todo-input",
            style := "width: 100%; padding: 10px; font-size: 1rem; border: 2px solid #cbd5e0; border-radius: 4px;"
          )
        )
      ),

      // Filter buttons
      div(cls := "filters", style := "margin-bottom: 20px; text-align: center;")(
        clickButton(
          "All",
          "set-filter-all",
          extraAttrs = Seq(
            style := s"padding: 8px 16px; margin: 0 5px; background: ${if (state.filter == ShowAll) "#667eea" else "#e2e8f0"}; color: ${if (state.filter == ShowAll) "white" else "#2d3748"}; border: none; border-radius: 4px; cursor: pointer;"
          )
        ),
        clickButton(
          "Active",
          "set-filter-active",
          extraAttrs = Seq(
            style := s"padding: 8px 16px; margin: 0 5px; background: ${if (state.filter == ShowActive) "#667eea" else "#e2e8f0"}; color: ${if (state.filter == ShowActive) "white" else "#2d3748"}; border: none; border-radius: 4px; cursor: pointer;"
          )
        ),
        clickButton(
          "Completed",
          "set-filter-completed",
          extraAttrs = Seq(
            style := s"padding: 8px 16px; margin: 0 5px; background: ${if (state.filter == ShowCompleted) "#667eea" else "#e2e8f0"}; color: ${if (state.filter == ShowCompleted) "white" else "#2d3748"}; border: none; border-radius: 4px; cursor: pointer;"
          )
        )
      ),

      // Todo list
      if (filteredItems.isEmpty) {
        div(cls := "empty-state", style := "text-align: center; padding: 40px; color: #a0aec0;")(
          Html.Text("No todos to show")
        )
      } else {
        keyedList(
          filteredItems,
          renderItem = (item, _) => renderTodoItem(item)
        )
      },

      // Clear completed button
      if (completedCount > 0) {
        div(style := "text-align: center; margin-top: 20px;")(
          clickButton(
            s"Clear completed ($completedCount)",
            "clear-completed",
            extraAttrs = Seq(
              style := "padding: 8px 16px; background: #f56565; color: white; border: none; border-radius: 4px; cursor: pointer;"
            )
          )
        )
      } else {
        Html.Empty
      }
    ).render
  }

  // ===== Helper Methods =====

  private def filterItems(items: List[TodoItem], filter: TodoFilter): List[TodoItem] = filter match {
    case ShowAll => items
    case ShowActive => items.filterNot(_.completed)
    case ShowCompleted => items.filter(_.completed)
  }

  private def renderTodoItem(item: TodoItem): Html = {
    div(cls := "todo-item", style := "display: flex; align-items: center; padding: 12px; border: 1px solid #cbd5e0; border-radius: 4px; margin-bottom: 8px;")(
      checkbox(
        s"todo-${item.id}",
        item.completed,
        s"toggle-${item.id}",
        labelText = None,
        extraAttrs = Seq(
          style := "margin-right: 12px; width: 20px; height: 20px; cursor: pointer;"
        )
      ),
      div(style := s"flex: 1; ${if (item.completed) "text-decoration: line-through; color: #a0aec0;" else ""}")(
        Html.Text(item.text)
      ),
      clickButton(
        "×",
        s"delete-${item.id}",
        extraAttrs = Seq(
          style := "background: none; border: none; color: #f56565; font-size: 1.5rem; cursor: pointer; padding: 0 8px;"
        )
      )
    )
  }
}

// ===== EventCodec for TodoFilter =====

given EventCodec[TodoFilter] = EventCodec.from[TodoFilter](
  encodeFunc = filter => dev.alteration.branch.friday.Json.JsonString(filter match {
    case ShowAll => "all"
    case ShowActive => "active"
    case ShowCompleted => "completed"
  }),
  decodeFunc = json => scala.util.Try {
    json match {
      case dev.alteration.branch.friday.Json.JsonString(s) => s match {
        case "all" => ShowAll
        case "active" => ShowActive
        case "completed" => ShowCompleted
        case _ => throw new RuntimeException(s"Unknown filter: $s")
      }
      case _ => throw new RuntimeException(s"Expected string, got: $json")
    }
  },
  decodeClientFunc = (eventName, payload) => scala.util.Try {
    eventName match {
      case "all" => ShowAll
      case "active" => ShowActive
      case "completed" => ShowCompleted
      case _ => throw new RuntimeException(s"Unknown filter: $eventName")
    }
  }
)

// ===== Tests =====

class TodoListWebViewTest extends WebViewSuite {

  test("TodoList mounts with initial items") {
    val webView = new TodoListWebView()
    val state = mount(webView)

    assertEquals(state.items.length, 2)
    assertEquals(state.filter, ShowAll)
    assertEquals(state.nextId, 3)
  }

  test("TodoList adds new item") {
    val webView = new TodoListWebView()
    val state = mount(webView)

    val newState = sendEvent(webView, AddTodo("Test todo"), state)

    assertEquals(newState.items.length, 3)
    assertEquals(newState.items.last.text, "Test todo")
    assertEquals(newState.nextId, 4)
  }

  test("TodoList toggles item completion") {
    val webView = new TodoListWebView()
    val state = mount(webView)

    val newState = sendEvent(webView, ToggleTodo(1), state)

    assert(newState.items.head.completed)
  }

  test("TodoList deletes item") {
    val webView = new TodoListWebView()
    val state = mount(webView)

    val newState = sendEvent(webView, DeleteTodo(1), state)

    assertEquals(newState.items.length, 1)
    assert(!newState.items.exists(_.id == 1))
  }

  test("TodoList filters items") {
    val webView = new TodoListWebView()
    var state = mount(webView)

    // Mark one as completed
    state = sendEvent(webView, ToggleTodo(1), state)

    // Filter to show only active
    state = sendEvent(webView, SetFilter(ShowActive), state)
    assertEquals(state.filter, ShowActive)

    val html = render(webView, state)
    assertHtmlContains(html, "Build awesome apps")
    // Note: filtering happens in render logic, so we'd need to check rendered output
  }

  test("TodoList clears completed items") {
    val webView = new TodoListWebView()
    var state = mount(webView)

    // Mark first item as completed
    state = sendEvent(webView, ToggleTodo(1), state)
    assertEquals(state.items.count(_.completed), 1)

    // Clear completed
    state = sendEvent(webView, ClearCompleted, state)
    assertEquals(state.items.length, 1)
    assertEquals(state.items.count(_.completed), 0)
  }

  test("TodoList renders with HTML DSL") {
    val webView = new TodoListWebView()
    val state = mount(webView)

    val html = render(webView, state)

    assertHtmlContains(html, "Todo List")
    assertHtmlContains(html, "Learn Branch WebView")
    assertHtmlContains(html, "Build awesome apps")
  }

  test("TodoList handles multiple events") {
    val webView = new TodoListWebView()
    val state = mount(webView)

    val finalState = sendEvents(
      webView,
      state,
      Seq(
        AddTodo("Task 1"),
        AddTodo("Task 2"),
        ToggleTodo(1),
        DeleteTodo(2)
      )
    )

    assertEquals(finalState.items.length, 3)
    assert(finalState.items.head.completed)
    assert(!finalState.items.exists(_.id == 2))
  }
}
