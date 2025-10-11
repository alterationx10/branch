package dev.alteration.branch.spider.webview

import java.time.Instant
import java.util.UUID

/** Domain models for the TodoList application */
case class Todo(
    id: String,
    text: String,
    completed: Boolean,
    createdAt: Instant
)

case class Filter(name: String, predicate: Todo => Boolean)

/** Application state with multiple component concerns */
case class TodoListState(
    todos: List[Todo],
    currentFilter: String,
    newTodoInput: String,
    editingId: Option[String],
    editingText: String,
    stats: TodoStats
)

case class TodoStats(
    total: Int,
    completed: Int,
    active: Int,
    percentComplete: Double
)

object TodoStats {
  def fromTodos(todos: List[Todo]): TodoStats = {
    val total = todos.length
    val completed = todos.count(_.completed)
    val active = total - completed
    val percentComplete = if (total > 0) (completed.toDouble / total) * 100 else 0.0
    TodoStats(total, completed, active, percentComplete)
  }
}

/** A multi-component TodoList WebView demonstrating:
  *   - Multiple interactive sections (header, list, filters, stats)
  *   - Complex state management
  *   - Component composition
  *   - Conditional rendering
  *
  * Components:
  *   1. Header - Add new todos
  *   2. TodoList - Display and manage todos
  *   3. Filters - Filter by status
  *   4. Stats - Show statistics
  */
class TodoListWebView extends WebView[TodoListState] {

  private val filters = Map(
    "all" -> Filter("All", _ => true),
    "active" -> Filter("Active", !_.completed),
    "completed" -> Filter("Completed", _.completed)
  )

  override def mount(
      params: Map[String, String],
      session: Session
  ): TodoListState = {
    // Start with some sample todos
    val sampleTodos = List(
      Todo(UUID.randomUUID().toString, "Learn Branch WebView", false, Instant.now()),
      Todo(UUID.randomUUID().toString, "Build amazing apps", false, Instant.now()),
      Todo(UUID.randomUUID().toString, "Ship to production", false, Instant.now())
    )

    TodoListState(
      todos = sampleTodos,
      currentFilter = "all",
      newTodoInput = "",
      editingId = None,
      editingText = "",
      stats = TodoStats.fromTodos(sampleTodos)
    )
  }

  override def handleEvent(
      event: String,
      payload: Map[String, Any],
      state: TodoListState
  ): TodoListState = {
    val newState = event match {
      // Header component events
      case "update-input" =>
        val text = payload.get("value").map(_.toString).getOrElse("")
        state.copy(newTodoInput = text)

      case "add-todo" =>
        if (state.newTodoInput.trim.nonEmpty) {
          val newTodo = Todo(
            id = UUID.randomUUID().toString,
            text = state.newTodoInput.trim,
            completed = false,
            createdAt = Instant.now()
          )
          val newTodos = state.todos :+ newTodo
          state.copy(
            todos = newTodos,
            newTodoInput = "",
            stats = TodoStats.fromTodos(newTodos)
          )
        } else {
          state
        }

      // TodoList component events
      case "toggle-todo" =>
        val todoId = payload.get("target").map(_.toString).getOrElse("")
        val newTodos = state.todos.map { todo =>
          if (todo.id == todoId) todo.copy(completed = !todo.completed)
          else todo
        }
        state.copy(
          todos = newTodos,
          stats = TodoStats.fromTodos(newTodos)
        )

      case "delete-todo" =>
        val todoId = payload.get("target").map(_.toString).getOrElse("")
        val newTodos = state.todos.filterNot(_.id == todoId)
        state.copy(
          todos = newTodos,
          stats = TodoStats.fromTodos(newTodos)
        )

      case "start-edit" =>
        val todoId = payload.get("target").map(_.toString).getOrElse("")
        state.todos.find(_.id == todoId) match {
          case Some(todo) =>
            state.copy(
              editingId = Some(todoId),
              editingText = todo.text
            )
          case None => state
        }

      case "update-edit" =>
        val text = payload.get("value").map(_.toString).getOrElse("")
        state.copy(editingText = text)

      case "save-edit" =>
        state.editingId match {
          case Some(todoId) if state.editingText.trim.nonEmpty =>
            val newTodos = state.todos.map { todo =>
              if (todo.id == todoId) todo.copy(text = state.editingText.trim)
              else todo
            }
            state.copy(
              todos = newTodos,
              editingId = None,
              editingText = ""
            )
          case _ =>
            state.copy(editingId = None, editingText = "")
        }

      case "cancel-edit" =>
        state.copy(editingId = None, editingText = "")

      // Filter component events
      case "set-filter" =>
        val filterName = payload.get("value").map(_.toString).getOrElse("all")
        state.copy(currentFilter = filterName)

      // Batch operations
      case "clear-completed" =>
        val newTodos = state.todos.filterNot(_.completed)
        state.copy(
          todos = newTodos,
          stats = TodoStats.fromTodos(newTodos)
        )

      case "toggle-all" =>
        val allCompleted = state.todos.forall(_.completed)
        val newTodos = state.todos.map(_.copy(completed = !allCompleted))
        state.copy(
          todos = newTodos,
          stats = TodoStats.fromTodos(newTodos)
        )

      case _ =>
        state
    }

    println(s"Event: $event => Stats: ${newState.stats}")
    newState
  }

  override def render(state: TodoListState): String = {
    val filteredTodos = filters
      .get(state.currentFilter)
      .map(_.predicate)
      .map(predicate => state.todos.filter(predicate))
      .getOrElse(state.todos)

    s"""
      <!DOCTYPE html>
      <html>
      <head>
        <style>
          body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            margin: 0;
            padding: 20px;
            min-height: 100vh;
          }
          .container {
            max-width: 600px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            overflow: hidden;
          }
          .header {
            background: #667eea;
            color: white;
            padding: 30px;
            text-align: center;
          }
          .header h1 {
            margin: 0 0 10px 0;
            font-size: 32px;
          }
          .header p {
            margin: 0;
            opacity: 0.9;
            font-size: 14px;
          }
          ${renderHeaderStyles()}
          ${renderTodoListStyles()}
          ${renderFilterStyles()}
          ${renderStatsStyles()}
        </style>
      </head>
      <body>
        <div class="container">
          ${renderHeader(state)}
          ${renderStats(state.stats)}
          ${renderFilters(state.currentFilter)}
          ${renderTodoList(filteredTodos, state)}
          ${renderFooter(state)}
        </div>
      </body>
      </html>
    """
  }

  /** Header Component - Add new todos */
  private def renderHeader(state: TodoListState): String = {
    s"""
      <div class="header">
        <h1>Branch TodoList</h1>
        <p>A multi-component WebView demo</p>
      </div>
      <div class="add-todo">
        <input
          type="text"
          id="new-todo-input"
          wv-change="update-input"
          value="${escapeHtml(state.newTodoInput)}"
          placeholder="What needs to be done?"
          class="todo-input"
        />
        <button wv-click="add-todo" class="add-btn">Add</button>
      </div>
    """
  }

  private def renderHeaderStyles(): String = {
    """
      .add-todo {
        padding: 20px;
        display: flex;
        gap: 10px;
        border-bottom: 2px solid #f0f0f0;
      }
      .todo-input {
        flex: 1;
        padding: 12px;
        border: 2px solid #e0e0e0;
        border-radius: 6px;
        font-size: 16px;
        transition: border-color 0.2s;
      }
      .todo-input:focus {
        outline: none;
        border-color: #667eea;
      }
      .add-btn {
        padding: 12px 24px;
        background: #667eea;
        color: white;
        border: none;
        border-radius: 6px;
        font-size: 16px;
        cursor: pointer;
        transition: background 0.2s;
      }
      .add-btn:hover {
        background: #5568d3;
      }
    """
  }

  /** Stats Component - Show statistics */
  private def renderStats(stats: TodoStats): String = {
    s"""
      <div class="stats">
        <div class="stat-item">
          <div class="stat-value">${stats.total}</div>
          <div class="stat-label">Total</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">${stats.active}</div>
          <div class="stat-label">Active</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">${stats.completed}</div>
          <div class="stat-label">Done</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">${f"${stats.percentComplete}%.0f"}%</div>
          <div class="stat-label">Complete</div>
        </div>
      </div>
      <div class="progress-bar">
        <div class="progress-fill" style="width: ${stats.percentComplete}%"></div>
      </div>
    """
  }

  private def renderStatsStyles(): String = {
    """
      .stats {
        display: flex;
        justify-content: space-around;
        padding: 20px;
        background: #f8f9fa;
      }
      .stat-item {
        text-align: center;
      }
      .stat-value {
        font-size: 24px;
        font-weight: bold;
        color: #667eea;
      }
      .stat-label {
        font-size: 12px;
        color: #666;
        margin-top: 4px;
      }
      .progress-bar {
        height: 4px;
        background: #e0e0e0;
      }
      .progress-fill {
        height: 100%;
        background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
        transition: width 0.3s ease;
      }
    """
  }

  /** Filter Component - Filter todos by status */
  private def renderFilters(currentFilter: String): String = {
    def filterButton(name: String, label: String) = {
      val activeClass = if (currentFilter == name) "active" else ""
      s"""<button wv-click="set-filter" value="$name" class="filter-btn $activeClass">$label</button>"""
    }

    s"""
      <div class="filters">
        ${filterButton("all", "All")}
        ${filterButton("active", "Active")}
        ${filterButton("completed", "Completed")}
      </div>
    """
  }

  private def renderFilterStyles(): String = {
    """
      .filters {
        display: flex;
        gap: 10px;
        padding: 15px 20px;
        border-bottom: 1px solid #e0e0e0;
      }
      .filter-btn {
        padding: 8px 16px;
        background: white;
        border: 2px solid #e0e0e0;
        border-radius: 6px;
        cursor: pointer;
        transition: all 0.2s;
        font-size: 14px;
      }
      .filter-btn:hover {
        border-color: #667eea;
        color: #667eea;
      }
      .filter-btn.active {
        background: #667eea;
        color: white;
        border-color: #667eea;
      }
    """
  }

  /** TodoList Component - Display and manage todos */
  private def renderTodoList(todos: List[Todo], state: TodoListState): String = {
    if (todos.isEmpty) {
      """
        <div class="empty-state">
          <div class="empty-icon">📝</div>
          <p>No todos to show</p>
        </div>
      """
    } else {
      val todoItems = todos.map(todo => renderTodoItem(todo, state)).mkString("\n")
      s"""<div class="todo-list">$todoItems</div>"""
    }
  }

  private def renderTodoItem(todo: Todo, state: TodoListState): String = {
    val isEditing = state.editingId.contains(todo.id)
    val completedClass = if (todo.completed) "completed" else ""

    if (isEditing) {
      s"""
        <div class="todo-item editing">
          <input
            type="text"
            id="edit-${todo.id}"
            wv-change="update-edit"
            value="${escapeHtml(state.editingText)}"
            class="edit-input"
          />
          <button wv-click="save-edit" class="save-btn">Save</button>
          <button wv-click="cancel-edit" class="cancel-btn">Cancel</button>
        </div>
      """
    } else {
      s"""
        <div class="todo-item $completedClass">
          <input
            type="checkbox"
            id="${todo.id}"
            wv-click="toggle-todo"
            ${if (todo.completed) "checked" else ""}
            class="todo-checkbox"
          />
          <span class="todo-text">${escapeHtml(todo.text)}</span>
          <div class="todo-actions">
            <button wv-click="start-edit" value="${todo.id}" class="edit-btn">Edit</button>
            <button wv-click="delete-todo" value="${todo.id}" class="delete-btn">×</button>
          </div>
        </div>
      """
    }
  }

  private def renderTodoListStyles(): String = {
    """
      .todo-list {
        max-height: 400px;
        overflow-y: auto;
      }
      .todo-item {
        display: flex;
        align-items: center;
        padding: 15px 20px;
        border-bottom: 1px solid #f0f0f0;
        transition: background 0.2s;
      }
      .todo-item:hover {
        background: #f8f9fa;
      }
      .todo-item.completed .todo-text {
        text-decoration: line-through;
        opacity: 0.5;
      }
      .todo-checkbox {
        width: 20px;
        height: 20px;
        cursor: pointer;
        margin-right: 12px;
      }
      .todo-text {
        flex: 1;
        font-size: 16px;
      }
      .todo-actions {
        display: flex;
        gap: 8px;
      }
      .edit-btn, .delete-btn, .save-btn, .cancel-btn {
        padding: 6px 12px;
        border: none;
        border-radius: 4px;
        cursor: pointer;
        font-size: 14px;
        transition: all 0.2s;
      }
      .edit-btn {
        background: #667eea;
        color: white;
      }
      .edit-btn:hover {
        background: #5568d3;
      }
      .delete-btn {
        background: #ff6b6b;
        color: white;
        font-size: 20px;
        padding: 4px 10px;
      }
      .delete-btn:hover {
        background: #ee5a52;
      }
      .save-btn {
        background: #51cf66;
        color: white;
      }
      .cancel-btn {
        background: #adb5bd;
        color: white;
      }
      .edit-input {
        flex: 1;
        padding: 8px 12px;
        border: 2px solid #667eea;
        border-radius: 4px;
        font-size: 16px;
        margin-right: 8px;
      }
      .empty-state {
        text-align: center;
        padding: 60px 20px;
        color: #999;
      }
      .empty-icon {
        font-size: 64px;
        margin-bottom: 20px;
      }
    """
  }

  /** Footer Component - Bulk actions */
  private def renderFooter(state: TodoListState): String = {
    val hasCompleted = state.todos.exists(_.completed)
    val clearButton = if (hasCompleted) {
      """<button wv-click="clear-completed" class="footer-btn">Clear Completed</button>"""
    } else ""

    val toggleAllButton = if (state.todos.nonEmpty) {
      """<button wv-click="toggle-all" class="footer-btn">Toggle All</button>"""
    } else ""

    if (clearButton.nonEmpty || toggleAllButton.nonEmpty) {
      s"""
        <div class="footer">
          $toggleAllButton
          $clearButton
        </div>
        <style>
          .footer {
            padding: 15px 20px;
            display: flex;
            gap: 10px;
            justify-content: center;
            border-top: 2px solid #f0f0f0;
            background: #f8f9fa;
          }
          .footer-btn {
            padding: 8px 16px;
            background: white;
            border: 2px solid #e0e0e0;
            border-radius: 6px;
            cursor: pointer;
            font-size: 14px;
            transition: all 0.2s;
          }
          .footer-btn:hover {
            border-color: #667eea;
            color: #667eea;
          }
        </style>
      """
    } else ""
  }

  private def escapeHtml(text: String): String = {
    text
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")
  }

  override def terminate(reason: Option[Throwable], state: TodoListState): Unit = {
    println(s"TodoListWebView terminated. ${state.stats.total} todos")
  }
}
