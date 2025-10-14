package spider.webview

import dev.alteration.branch.spider.webview.*
import dev.alteration.branch.spider.webview.html.Tags.{*, given}
import dev.alteration.branch.spider.webview.html.Attributes.*
import dev.alteration.branch.spider.webview.html.WebViewAttributes.*
import scala.language.implicitConversions

/** A todo list WebView example.
  *
  * This demonstrates more advanced WebView features:
  *   - Managing a list in state
  *   - Multiple event types with parameters
  *   - Dynamic rendering based on state
  *   - Input handling
  *   - Conditional rendering
  */

// State containing the list of todos
case class TodoState(
    todos: List[TodoItem] = List.empty,
    nextId: Int = 1
)

case class TodoItem(id: Int, text: String, completed: Boolean)

// Events for the todo list
sealed trait TodoEvent derives EventCodec
case class AddTodo(text: String) extends TodoEvent
case class ToggleTodo(id: Int)   extends TodoEvent
case class DeleteTodo(id: Int)   extends TodoEvent
case object ClearCompleted       extends TodoEvent

// The TodoList WebView
class TodoWebView extends WebView[TodoState, TodoEvent] {

  override def mount(
      params: Map[String, String],
      session: Session
  ): TodoState = {
    // Start with some example todos
    TodoState(
      todos = List(
        TodoItem(1, "Learn Branch Spider", false),
        TodoItem(2, "Build a WebView app", false),
        TodoItem(3, "Deploy to production", false)
      ),
      nextId = 4
    )
  }

  override def handleEvent(event: TodoEvent, state: TodoState): TodoState = {
    event match {
      case AddTodo(text) if text.trim.nonEmpty =>
        val newTodo = TodoItem(state.nextId, text.trim, false)
        state.copy(
          todos = state.todos :+ newTodo,
          nextId = state.nextId + 1
        )

      case AddTodo(_) =>
        // Ignore empty todos
        state

      case ToggleTodo(id) =>
        state.copy(
          todos = state.todos.map { todo =>
            if (todo.id == id) todo.copy(completed = !todo.completed)
            else todo
          }
        )

      case DeleteTodo(id) =>
        state.copy(todos = state.todos.filterNot(_.id == id))

      case ClearCompleted =>
        state.copy(todos = state.todos.filterNot(_.completed))
    }
  }

  override def render(state: TodoState): String = {
    val totalTodos     = state.todos.length
    val completedTodos = state.todos.count(_.completed)
    val activeTodos    = totalTodos - completedTodos

    val todosHtml = if (state.todos.isEmpty) {
      """<div style="text-align: center; color: #a0aec0; padding: 40px;">
           No todos yet. Add one above!
         </div>"""
    } else {
      state.todos.map { todo =>
        // We don't have to just interpolate wads of HTML string - we can use our Html dsl
        // Note for wv-click, we can encode the event directly, and it will build the appropriate json
        div(
          styles(
            "display"       -> "flex",
            "align-items"   -> "center",
            "padding"       -> "15px",
            "border-bottom" -> "1px solid #e2e8f0"
          )
        )(
          input(
            tpe     := "checkbox",
            checked := todo.completed,
            wvClick := EventCodec[TodoEvent]
              .encode(ToggleTodo(todo.id))
              .toJsonString,
            styles(
              "width"        -> "20px",
              "height"       -> "20px",
              "margin-right" -> "15px",
              "cursor"       -> "pointer"
            )
          ),
          span(
            styleWhen(
              ("flex", "1", true),
              ("font-size", "1.1rem", true),
              ("text-decoration", "line-through", todo.completed),
              ("color", if (todo.completed) "#a0aec0" else "#2d3748", true)
            )
          )(
            escapeHtml(todo.text)
          ),
          button(
            wvClick := EventCodec[TodoEvent]
              .encode(DeleteTodo(todo.id))
              .toJsonString,
            styles(
              "padding"       -> "8px 12px",
              "background"    -> "#f56565",
              "color"         -> "white",
              "border"        -> "none",
              "border-radius" -> "6px",
              "cursor"        -> "pointer",
              "font-size"     -> "0.9rem"
            )
          )(
            "Delete"
          )
        ).render
      }.mkString
    }

    val clearCompletedButton = when(completedTodos > 0) {
      button(
        wvClick := EventCodec[TodoEvent].encode(ClearCompleted).toJsonString,
        styles(
          "padding"       -> "10px 20px",
          "background"    -> "#ed8936",
          "color"         -> "white",
          "border"        -> "none",
          "border-radius" -> "8px",
          "cursor"        -> "pointer"
        )
      )(
        "Clear Completed"
      )
    }.render

    // But you can still use HTML directly if you want, or even render mustache templates!
    s"""
    <div style="font-family: sans-serif; max-width: 600px; margin: 50px auto; background: white; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
      <div style="padding: 30px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 12px 12px 0 0; color: white;">
        <h1 style="margin: 0; font-size: 2rem;">Todo List</h1>
        <p style="margin: 10px 0 0 0; opacity: 0.9;">Manage your tasks with Branch WebView</p>
      </div>

      <!--Note that the wv-submit is AddTodo, and the input name="text" -->
      <!--This is then build as AddTodo(text)...-->
      <div style="padding: 30px;">
        <form wv-submit="AddTodo"
              style="display: flex; gap: 10px; margin-bottom: 20px;">
          <input name="text" type="text" placeholder="What needs to be done?"
                 style="flex: 1; padding: 12px; font-size: 1rem; border: 2px solid #e2e8f0; border-radius: 8px;">
          <button type="submit"
                  style="padding: 12px 24px; background: #48bb78; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: bold;">
            Add
          </button>
        </form>

        <div style="background: #f7fafc; border-radius: 8px; overflow: hidden;">
          $todosHtml
        </div>

        <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 20px; padding-top: 20px; border-top: 1px solid #e2e8f0;">
          <div style="color: #718096;">
            <span style="font-weight: bold; color: #2d3748;">$activeTodos</span> active,
            <span style="font-weight: bold; color: #2d3748;">$completedTodos</span> completed
          </div>
          $clearCompletedButton
        </div>
      </div>
    </div>
    """
  }

  private def escapeHtml(text: String): String = {
    text
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")
  }
}
