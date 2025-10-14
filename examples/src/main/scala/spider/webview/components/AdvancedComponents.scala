package spider.webview.components

import dev.alteration.branch.spider.webview.html.Html
import dev.alteration.branch.spider.webview.html.Tags.*
import dev.alteration.branch.spider.webview.html.Attributes.*
import dev.alteration.branch.spider.webview.styling.CSSUtils.*

/** Advanced reusable UI components for Branch WebView.
  *
  * Provides higher-level components like tables, modals, tabs, etc.
  *
  * These are example components that demonstrate how to build reusable UI elements.
  * Feel free to copy and adapt them for your own applications.
  */
object AdvancedComponents {

  // ===== Data Table Component =====

  /** Configuration for a table column.
    *
    * @param key
    *   The key to extract data from rows
    * @param header
    *   The header text to display
    * @param sortable
    *   Whether this column can be sorted
    * @param render
    *   Optional custom renderer for cell values
    */
  case class TableColumn[T](
      key: String,
      header: String,
      sortable: Boolean = false,
      render: Option[T => Html] = None
  )

  /** Data table with sorting and optional filtering.
    *
    * @param columns
    *   Column definitions
    * @param rows
    *   Data rows
    * @param sortColumn
    *   Currently sorted column (if any)
    * @param sortAscending
    *   Sort direction
    * @param onSort
    *   Function to generate event JSON for sorting
    * @param extractValue
    *   Function to extract value from row by key
    * @tparam T
    *   Type of row data
    * @return
    *   Html table element
    */
  def dataTable[T](
      columns: List[TableColumn[T]],
      rows: List[T],
      sortColumn: Option[String] = None,
      sortAscending: Boolean = true,
      onSort: String => String,
      extractValue: (T, String) => String
  ): Html = {
    table(
      style := "width: 100%; border-collapse: collapse; font-family: sans-serif;"
    )(
      // Header
      thead(style := "background: #f7fafc; border-bottom: 2px solid #e2e8f0;")(
        tr()(
          columns.map { col =>
            th(
              style := s"padding: 12px; text-align: left; font-weight: 600; color: #2d3748; ${if (col.sortable) "cursor: pointer;" else ""}",
              if (col.sortable) attr("wv-click") := onSort(col.key)
              else attr("data-key")              := col.key
            )(
              div(style := "display: flex; align-items: center; gap: 8px;")(
                Html.Text(col.header),
                if (col.sortable && sortColumn.contains(col.key)) {
                  Html.Text(if (sortAscending) "▲" else "▼")
                } else if (col.sortable) {
                  Html.Text("⇅")
                } else {
                  Html.Empty
                }
              )
            )
          }*
        )
      ),
      // Body
      tbody()(
        if (rows.isEmpty) {
          tr()(
            td(
              attr("colspan") := columns.length.toString,
              style           := "padding: 40px; text-align: center; color: #a0aec0;"
            )(
              Html.Text("No data available")
            )
          )
        } else {
          Html.Fragment(rows.map { row =>
            tr(
              style := "border-bottom: 1px solid #e2e8f0; transition: background 150ms;"
            )(
              columns.map { col =>
                td(style := "padding: 12px; color: #4a5568;")(
                  col.render match {
                    case Some(renderer) => renderer(row)
                    case None           => Html.Text(extractValue(row, col.key))
                  }
                )
              }*
            )
          })
        }
      )
    )
  }

  // ===== Modal Dialog Component =====

  /** Modal dialog overlay.
    *
    * @param isOpen
    *   Whether the modal is visible
    * @param onClose
    *   Event JSON string to close the modal
    * @param title
    *   Modal title
    * @param content
    *   Modal body content
    * @param footer
    *   Optional footer content
    * @return
    *   Html modal structure
    */
  def modal(
      isOpen: Boolean,
      onClose: String,
      title: String,
      content: Html,
      footer: Option[Html] = None
  ): Html = {
    if (!isOpen) Html.Empty
    else {
      div(
        style            := "position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000;",
        attr("wv-click") := onClose
      )(
        div(
          style           := "background: white; border-radius: 12px; box-shadow: 0 20px 25px rgba(0,0,0,0.15); max-width: 600px; width: 90%; max-height: 90vh; overflow: auto;",
          attr("onclick") := "event.stopPropagation()"
        )(
          // Header
          div(
            style := "padding: 20px; border-bottom: 1px solid #e2e8f0; display: flex; justify-content: space-between; align-items: center;"
          )(
            h2(style := "margin: 0; font-size: 1.5rem; color: #2d3748;")(
              Html.Text(title)
            ),
            button(
              style            := "background: none; border: none; font-size: 1.5rem; cursor: pointer; color: #a0aec0; padding: 0; width: 32px; height: 32px;",
              attr("wv-click") := onClose
            )(Html.Text("×"))
          ),
          // Body
          div(style := "padding: 20px;")(content),
          // Footer
          footer match {
            case Some(footerContent) =>
              div(
                style := "padding: 20px; border-top: 1px solid #e2e8f0; display: flex; justify-content: flex-end; gap: 10px;"
              )(
                footerContent
              )
            case None                => Html.Empty
          }
        )
      )
    }
  }

  // ===== Dropdown Menu Component =====

  /** Dropdown menu.
    *
    * @param isOpen
    *   Whether dropdown is visible
    * @param trigger
    *   The element that triggers the dropdown
    * @param items
    *   List of menu items (label, event JSON string)
    * @return
    *   Html dropdown structure
    */
  def dropdown(
      isOpen: Boolean,
      trigger: Html,
      items: List[(String, String)]
  ): Html = {
    div(style := "position: relative; display: inline-block;")(
      trigger,
      if (isOpen) {
        div(
          style := "position: absolute; top: 100%; left: 0; margin-top: 8px; background: white; border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: 0 10px 15px rgba(0,0,0,0.1); min-width: 200px; z-index: 100;"
        )(
          items.map { case (label, event) =>
            div(
              style            := "padding: 12px 16px; cursor: pointer; color: #2d3748; transition: background 150ms; border-bottom: 1px solid #f7fafc;",
              attr("wv-click") := event
            )(Html.Text(label))
          }*
        )
      } else {
        Html.Empty
      }
    )
  }

  // ===== Tabs Component =====

  /** Tab navigation.
    *
    * @param tabs
    *   List of tab (id, label) pairs
    * @param activeTab
    *   Currently active tab id
    * @param onTabChange
    *   Function to generate event JSON for tab change
    * @return
    *   Html tab navigation
    */
  def tabs(
      tabs: List[(String, String)],
      activeTab: String,
      onTabChange: String => String
  ): Html = {
    div(style := "border-bottom: 2px solid #e2e8f0;")(
      div(style := "display: flex; gap: 4px;")(
        tabs.map { case (id, label) =>
          val isActive = id == activeTab
          button(
            style            := s"padding: 12px 24px; border: none; background: ${
                if (isActive) "white" else "transparent"
              }; color: ${if (isActive) Colors.primary else "#718096"}; border-bottom: ${
                if (isActive) s"3px solid ${Colors.primary}"
                else "3px solid transparent"
              }; cursor: pointer; font-size: 1rem; font-weight: ${
                if (isActive) "600" else "normal"
              }; transition: all 150ms;",
            attr("wv-click") := s"${onTabChange(id)}"
          )(Html.Text(label))
        }*
      )
    )
  }

  /** Tab panel content.
    *
    * @param tabId
    *   This panel's tab id
    * @param activeTab
    *   Currently active tab id
    * @param content
    *   Panel content
    * @return
    *   Html panel (hidden if not active)
    */
  def tabPanel(
      tabId: String,
      activeTab: String,
      content: Html
  ): Html = {
    if (tabId == activeTab) {
      div(style := "padding: 20px 0;")(content)
    } else {
      Html.Empty
    }
  }

  // ===== Accordion Component =====

  /** Accordion item.
    *
    * @param id
    *   Unique id for this item
    * @param title
    *   Item title
    * @param content
    *   Item content
    * @param isOpen
    *   Whether item is expanded
    * @param onToggle
    *   Function to generate event JSON for toggle
    * @return
    *   Html accordion item
    */
  def accordionItem(
      id: String,
      title: String,
      content: Html,
      isOpen: Boolean,
      onToggle: String => String
  ): Html = {
    div(
      style := "border: 1px solid #e2e8f0; border-radius: 8px; margin-bottom: 8px; overflow: hidden;"
    )(
      // Header
      div(
        style            := s"padding: 16px; background: ${
            if (isOpen) "#f7fafc" else "white"
          }; cursor: pointer; display: flex; justify-content: space-between; align-items: center;",
        attr("wv-click") := s"${onToggle(id)}"
      )(
        div(style := "font-weight: 600; color: #2d3748;")(Html.Text(title)),
        div(style := s"color: ${Colors.primary}; font-size: 1.2rem;")(
          Html.Text(if (isOpen) "−" else "+")
        )
      ),
      // Content
      if (isOpen) {
        div(
          style := "padding: 16px; border-top: 1px solid #e2e8f0; background: white;"
        )(
          content
        )
      } else {
        Html.Empty
      }
    )
  }

  // ===== Card Component =====

  /** Card component with optional header and footer.
    *
    * @param title
    *   Optional card title
    * @param content
    *   Card body content
    * @param footer
    *   Optional footer content
    * @param variant
    *   Style variant (default, primary, success, danger, warning)
    * @return
    *   Html card
    */
  def card(
      title: Option[String] = None,
      content: Html,
      footer: Option[Html] = None,
      variant: String = "default"
  ): Html = {
    val borderColor = variant match {
      case "primary" => Colors.primary
      case "success" => Colors.success
      case "danger"  => Colors.danger
      case "warning" => Colors.warning
      case _         => "#e2e8f0"
    }

    div(
      style := s"background: white; border: 1px solid $borderColor; border-radius: 8px; box-shadow: ${Shadows.sm}; overflow: hidden;"
    )(
      title match {
        case Some(t) =>
          div(
            style := "padding: 16px 20px; border-bottom: 1px solid #e2e8f0; background: #f7fafc;"
          )(
            h3(style := "margin: 0; font-size: 1.25rem; color: #2d3748;")(
              Html.Text(t)
            )
          )
        case None    => Html.Empty
      },
      div(style := "padding: 20px;")(content),
      footer match {
        case Some(f) =>
          div(
            style := "padding: 16px 20px; border-top: 1px solid #e2e8f0; background: #f7fafc;"
          )(f)
        case None    => Html.Empty
      }
    )
  }

  // ===== Badge Component =====

  /** Badge/label component.
    *
    * @param text
    *   Badge text
    * @param variant
    *   Color variant (primary, success, danger, warning, info, default)
    * @return
    *   Html badge
    */
  def badge(text: String, variant: String = "default"): Html = {
    val (bg, color) = variant match {
      case "primary" => (Colors.primary, "white")
      case "success" => (Colors.success, "white")
      case "danger"  => (Colors.danger, "white")
      case "warning" => (Colors.warning, "white")
      case "info"    => (Colors.info, "white")
      case _         => (Colors.light, Colors.dark)
    }

    span(
      style := s"display: inline-block; padding: 4px 12px; background: $bg; color: $color; border-radius: ${Radius.full}; font-size: 0.875rem; font-weight: 600;"
    )(
      Html.Text(text)
    )
  }

  // ===== Alert Component =====

  /** Alert/notification box.
    *
    * @param message
    *   Alert message
    * @param variant
    *   Alert type (success, warning, danger, info)
    * @param dismissible
    *   Whether alert can be dismissed
    * @param onDismiss
    *   Event JSON string for dismissing
    * @return
    *   Html alert
    */
  def alert(
      message: String,
      variant: String = "info",
      dismissible: Boolean = false,
      onDismiss: String = "dismiss-alert"
  ): Html = {
    val (bg, border, text, icon) = variant match {
      case "success" => ("#d4edda", "#c3e6cb", "#155724", "✓")
      case "warning" => ("#fff3cd", "#ffeaa7", "#856404", "⚠")
      case "danger"  => ("#f8d7da", "#f5c6cb", "#721c24", "✕")
      case _         => ("#d1ecf1", "#bee5eb", "#0c5460", "ℹ")
    }

    div(
      style := s"padding: 16px; background: $bg; border: 1px solid $border; border-radius: 8px; color: $text; display: flex; justify-content: space-between; align-items: start; gap: 12px;"
    )(
      div(style := "display: flex; gap: 12px; flex: 1;")(
        div(style := "font-weight: bold; font-size: 1.2rem;")(Html.Text(icon)),
        div(style := "flex: 1;")(Html.Text(message))
      ),
      if (dismissible) {
        button(
          style            := "background: none; border: none; color: inherit; cursor: pointer; font-size: 1.2rem; padding: 0;",
          attr("wv-click") := onDismiss
        )(Html.Text("×"))
      } else {
        Html.Empty
      }
    )
  }

  // ===== Progress Bar Component =====

  /** Progress bar.
    *
    * @param value
    *   Progress value (0-100)
    * @param label
    *   Optional label to display
    * @param variant
    *   Color variant (primary, success, danger, warning)
    * @return
    *   Html progress bar
    */
  def progressBar(
      value: Int,
      label: Option[String] = None,
      variant: String = "primary"
  ): Html = {
    val color = variant match {
      case "success" => Colors.success
      case "danger"  => Colors.danger
      case "warning" => Colors.warning
      case _         => Colors.primary
    }

    val clampedValue = math.max(0, math.min(100, value))

    div()(
      label match {
        case Some(l) =>
          div(
            style := "margin-bottom: 8px; font-size: 0.875rem; color: #4a5568; display: flex; justify-content: space-between;"
          )(
            Html.Text(l),
            Html.Text(s"$clampedValue%")
          )
        case None    => Html.Empty
      },
      div(
        style := s"width: 100%; height: 24px; background: #e2e8f0; border-radius: ${Radius.full}; overflow: hidden;"
      )(
        div(
          style := s"height: 100%; width: $clampedValue%; background: $color; transition: width 300ms ease; display: flex; align-items: center; justify-content: center; color: white; font-size: 0.75rem; font-weight: 600;"
        )(
          if (clampedValue > 10) Html.Text(s"$clampedValue%") else Html.Empty
        )
      )
    )
  }

  // ===== Pagination Component =====

  /** Pagination controls.
    *
    * @param currentPage
    *   Current page number (1-indexed)
    * @param totalPages
    *   Total number of pages
    * @param onPageChange
    *   Function to generate event JSON for page change
    * @return
    *   Html pagination
    */
  def pagination(
      currentPage: Int,
      totalPages: Int,
      onPageChange: String => String
  ): Html = {
    div(
      style := "display: flex; gap: 4px; justify-content: center; align-items: center;"
    )(
      Html.Fragment(
        List(
          // Previous button
          button(
            style            := s"padding: 8px 12px; border: 1px solid #e2e8f0; background: white; border-radius: 6px; cursor: ${
                if (currentPage <= 1) "not-allowed" else "pointer"
              }; color: ${if (currentPage <= 1) "#a0aec0" else Colors.primary};",
            attr("wv-click") := onPageChange(s"${math.max(1, currentPage - 1)}"),
            if (currentPage <= 1) attr("disabled") := "true"
            else attr("data-enabled")              := "true"
          )(Html.Text("← Prev"))
        ) ++
          // Page numbers (show current and 2 on each side)
          (math.max(1, currentPage - 2) to math
            .min(totalPages, currentPage + 2)).map { page =>
            button(
              style            := s"padding: 8px 12px; border: 1px solid ${
                  if (page == currentPage) Colors.primary else "#e2e8f0"
                }; background: ${
                  if (page == currentPage) Colors.primary else "white"
                }; color: ${if (page == currentPage) "white" else "#2d3748"}; border-radius: 6px; cursor: pointer; font-weight: ${if (page == currentPage) "600" else "normal"};",
              attr("wv-click") := onPageChange(page.toString)
            )(Html.Text(page.toString))
          } ++
          List(
            // Next button
            button(
              style            := s"padding: 8px 12px; border: 1px solid #e2e8f0; background: white; border-radius: 6px; cursor: ${if (currentPage >= totalPages) "not-allowed" else "pointer"}; color: ${if (currentPage >= totalPages) "#a0aec0" else Colors.primary};",
              attr("wv-click") := onPageChange(s"${math.min(totalPages, currentPage + 1)}"),
              if (currentPage >= totalPages) attr("disabled") := "true"
              else attr("data-enabled")                       := "true"
            )(Html.Text("Next →"))
          )
      )
    )
  }
}
