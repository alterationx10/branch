package spider.webview

import dev.alteration.branch.spider.webview.*
import dev.alteration.branch.spider.webview.html.Attributes.*
import dev.alteration.branch.spider.webview.html.Html
import dev.alteration.branch.spider.webview.html.Tags.*
import dev.alteration.branch.spider.webview.html.WebViewAttributes.*
import spider.webview.components.AdvancedComponents.*

import scala.language.implicitConversions

/** A comprehensive example showcasing all advanced UI components.
  *
  * This demonstrates:
  *   - Data tables with sorting
  *   - Modal dialogs
  *   - Tabs navigation
  *   - Accordion components
  *   - Cards and badges
  *   - Alerts and notifications
  *   - Progress bars
  *   - Pagination
  */

// Sample data for the table
case class User(
    id: Int,
    name: String,
    email: String,
    role: String,
    status: String
)

// State for the showcase
case class ShowcaseState(
    // Table state
    users: List[User] = List(
      User(1, "Alice Johnson", "alice@example.com", "Admin", "Active"),
      User(2, "Bob Smith", "bob@example.com", "User", "Active"),
      User(3, "Charlie Brown", "charlie@example.com", "User", "Inactive"),
      User(4, "Diana Prince", "diana@example.com", "Moderator", "Active"),
      User(5, "Eve Wilson", "eve@example.com", "User", "Active")
    ),
    sortColumn: Option[String] = None,
    sortAscending: Boolean = true,
    // Modal state
    isModalOpen: Boolean = false,
    // Dropdown state
    isDropdownOpen: Boolean = false,
    // Tabs state
    activeTab: String = "overview",
    // Accordion state
    openAccordionItems: Set[String] = Set("item1"),
    // Alert state
    showAlert: Boolean = true,
    // Progress state
    progressValue: Int = 45,
    // Pagination state
    currentPage: Int = 1,
    totalPages: Int = 5
)

// Events for the showcase
sealed trait ShowcaseEvent derives EventCodec
case class SortTable(column: String)          extends ShowcaseEvent
case object OpenModal                         extends ShowcaseEvent
case object CloseModal                        extends ShowcaseEvent
case object ToggleDropdown                    extends ShowcaseEvent
case class SwitchTab(tabId: String)           extends ShowcaseEvent
case class ToggleAccordion(itemId: String)    extends ShowcaseEvent
case object DismissAlert                      extends ShowcaseEvent
case object IncreaseProgress                  extends ShowcaseEvent
case object DecreaseProgress                  extends ShowcaseEvent
case class ChangePage(page: Int)              extends ShowcaseEvent
case class SelectDropdownItem(action: String) extends ShowcaseEvent

class ComponentsShowcaseWebView extends WebView[ShowcaseState, ShowcaseEvent] {

  override def mount(
      params: Map[String, String],
      session: Session
  ): ShowcaseState = ShowcaseState()

  override def handleEvent(
      event: ShowcaseEvent,
      state: ShowcaseState
  ): ShowcaseState = {
    event match {
      case SortTable(column) =>
        val newSortAscending =
          if (state.sortColumn.contains(column)) !state.sortAscending
          else true

        val sortedUsers = sortUsers(state.users, column, newSortAscending)

        state.copy(
          users = sortedUsers,
          sortColumn = Some(column),
          sortAscending = newSortAscending
        )

      case OpenModal  => state.copy(isModalOpen = true)
      case CloseModal => state.copy(isModalOpen = false)

      case ToggleDropdown => state.copy(isDropdownOpen = !state.isDropdownOpen)

      case SelectDropdownItem(action) =>
        // Close dropdown after selection
        state.copy(isDropdownOpen = false)

      case SwitchTab(tabId) =>
        println(s"switching to tab: $tabId")
        state.copy(activeTab = tabId)

      case ToggleAccordion(itemId) =>
        val newOpenItems =
          if (state.openAccordionItems.contains(itemId))
            state.openAccordionItems - itemId
          else state.openAccordionItems + itemId
        state.copy(openAccordionItems = newOpenItems)

      case DismissAlert => state.copy(showAlert = false)

      case IncreaseProgress =>
        state.copy(progressValue = math.min(100, state.progressValue + 10))

      case DecreaseProgress =>
        state.copy(progressValue = math.max(0, state.progressValue - 10))

      case ChangePage(page) =>
        state.copy(currentPage = math.max(1, math.min(state.totalPages, page)))
    }
  }

  override def render(state: ShowcaseState): String = {
    div(
      style := "font-family: sans-serif; max-width: 1200px; margin: 0 auto; padding: 20px; background: #f7fafc;"
    )(
      // Header
      div(
        style := "background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px; border-radius: 12px; color: white; margin-bottom: 30px;"
      )(
        h1(style := "margin: 0; font-size: 2.5rem;")(
          Html.Text("Branch Components Showcase")
        ),
        p(style := "margin: 10px 0 0 0; font-size: 1.1rem; opacity: 0.9;")(
          Html.Text(
            "A comprehensive demonstration of all advanced UI components"
          )
        )
      ),

      // Alert section
      if (state.showAlert) {
        div(style := "margin-bottom: 20px;")(
          alert(
            "Welcome to the components showcase! Explore all the pre-built components below.",
            variant = "info",
            dismissible = true,
            onDismiss =
              EventCodec[ShowcaseEvent].encode(DismissAlert).toJsonString
          )
        )
      } else Html.Empty,

      // Tabs Navigation
      tabs(
        tabs = List(
          ("overview", "Overview"),
          ("data", "Data & Tables"),
          ("interactive", "Interactive"),
          ("feedback", "Feedback")
        ),
        activeTab = state.activeTab,
        onTabChange = (id: String) =>
          EventCodec[ShowcaseEvent].encode(SwitchTab(id)).toJsonString
      ),

      // Tab: Overview
      tabPanel(
        tabId = "overview",
        activeTab = state.activeTab,
        content = div()(
          h2(style := "color: #2d3748; margin-bottom: 20px;")(
            Html.Text("Overview")
          ),

          // Cards showcase
          div(
            style := "display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; margin-bottom: 30px;"
          )(
            card(
              title = Some("Default Card"),
              content = div()(
                p()(
                  Html.Text("This is a default card with a title and content.")
                ),
                div(style := "margin-top: 10px;")(
                  badge("Info", "info"),
                  Html.Text(" "),
                  badge("Default", "default")
                )
              ),
              footer = Some(
                div(style := "display: flex; gap: 10px;")(
                  button(
                    style := "padding: 8px 16px; background: #667eea; color: white; border: none; border-radius: 6px; cursor: pointer;"
                  )(Html.Text("Action"))
                )
              )
            ),
            card(
              title = Some("Success Card"),
              content = div()(
                p()(
                  Html.Text(
                    "This card has a success variant with green styling."
                  )
                ),
                div(style := "margin-top: 10px;")(
                  badge("Active", "success")
                )
              ),
              variant = "success"
            ),
            card(
              title = Some("Warning Card"),
              content = div()(
                p()(Html.Text("This card uses the warning variant.")),
                div(style := "margin-top: 10px;")(
                  badge("Attention", "warning")
                )
              ),
              variant = "warning"
            )
          ),

          // Accordion
          h3(style := "color: #2d3748; margin: 30px 0 15px 0;")(
            Html.Text("Accordion Component")
          ),
          div()(
            accordionItem(
              id = "item1",
              title = "What is Branch WebView?",
              content = Html.Text(
                "Branch WebView is a server-side rendered UI framework that allows you to build interactive web applications using Scala."
              ),
              isOpen = state.openAccordionItems.contains("item1"),
              onToggle = id =>
                EventCodec[ShowcaseEvent]
                  .encode(ToggleAccordion(id))
                  .toJsonString
            ),
            accordionItem(
              id = "item2",
              title = "How does state management work?",
              content = Html.Text(
                "State is managed on the server. When events occur, the server updates the state and sends a new HTML patch to the client."
              ),
              isOpen = state.openAccordionItems.contains("item2"),
              onToggle = id =>
                EventCodec[ShowcaseEvent]
                  .encode(ToggleAccordion(id))
                  .toJsonString
            ),
            accordionItem(
              id = "item3",
              title = "What components are available?",
              content = Html.Text(
                "Branch provides tables, modals, tabs, accordions, cards, badges, alerts, progress bars, pagination, and more!"
              ),
              isOpen = state.openAccordionItems.contains("item3"),
              onToggle = id =>
                EventCodec[ShowcaseEvent]
                  .encode(ToggleAccordion(id))
                  .toJsonString
            )
          )
        )
      ),

      // Tab: Data & Tables
      tabPanel(
        tabId = "data",
        activeTab = state.activeTab,
        content = div()(
          h2(style := "color: #2d3748; margin-bottom: 20px;")(
            Html.Text("Data Tables")
          ),
          card(
            title = Some("User Management"),
            content = dataTable[User](
              columns = List(
                TableColumn("id", "ID", sortable = true),
                TableColumn("name", "Name", sortable = true),
                TableColumn("email", "Email", sortable = true),
                TableColumn(
                  "role",
                  "Role",
                  sortable = false,
                  render = Some(renderRole)
                ),
                TableColumn(
                  "status",
                  "Status",
                  sortable = true,
                  render = Some(renderStatus)
                )
              ),
              rows = state.users,
              sortColumn = state.sortColumn,
              sortAscending = state.sortAscending,
              onSort = id => EventCodec[ShowcaseEvent].encode(SortTable(id)).toJsonString,
              extractValue = extractUserValue
            ),
            footer = Some(
              div(style := "text-align: center; color: #718096;")(
                Html.Text(s"Showing ${state.users.length} users")
              )
            )
          )
        )
      ),

      // Tab: Interactive
      tabPanel(
        tabId = "interactive",
        activeTab = state.activeTab,
        content = div()(
          h2(style := "color: #2d3748; margin-bottom: 20px;")(
            Html.Text("Interactive Components")
          ),

          // Modal showcase
          div(style := "margin-bottom: 30px;")(
            card(
              title = Some("Modal Dialog"),
              content = div()(
                p()(
                  Html.Text("Click the button below to open a modal dialog.")
                ),
                button(
                  wvClick := EventCodec[ShowcaseEvent]
                    .encode(OpenModal)
                    .toJsonString,
                  style   := "padding: 10px 20px; background: #667eea; color: white; border: none; border-radius: 8px; cursor: pointer; margin-top: 10px;"
                )(Html.Text("Open Modal"))
              )
            ),
            modal(
              isOpen = state.isModalOpen,
              onClose =
                EventCodec[ShowcaseEvent].encode(CloseModal).toJsonString,
              title = "Example Modal",
              content = div()(
                p()(
                  Html.Text(
                    "This is a modal dialog. It can contain any content you want!"
                  )
                ),
                p(style := "color: #718096; font-size: 0.9rem;")(
                  Html.Text("Click outside or press the X to close.")
                )
              ),
              footer = Some(
                div()(
                  button(
                    wvClick := EventCodec[ShowcaseEvent]
                      .encode(CloseModal)
                      .toJsonString,
                    style   := "padding: 10px 20px; background: #718096; color: white; border: none; border-radius: 8px; cursor: pointer;"
                  )(Html.Text("Close"))
                )
              )
            )
          ),

          // Dropdown showcase
          div(style := "margin-bottom: 30px;")(
            card(
              title = Some("Dropdown Menu"),
              content = dropdown(
                isOpen = state.isDropdownOpen,
                trigger = button(
                  wvClick := EventCodec[ShowcaseEvent]
                    .encode(ToggleDropdown)
                    .toJsonString,
                  style   := "padding: 10px 20px; background: #667eea; color: white; border: none; border-radius: 8px; cursor: pointer;"
                )(
                  Html.Text(
                    if (state.isDropdownOpen) "Close Menu ▲" else "Open Menu ▼"
                  )
                ),
                items = List(
                  (
                    "Edit Profile",
                    EventCodec[ShowcaseEvent]
                      .encode(SelectDropdownItem("edit"))
                      .toJsonString
                  ),
                  (
                    "Settings",
                    EventCodec[ShowcaseEvent]
                      .encode(SelectDropdownItem("settings"))
                      .toJsonString
                  ),
                  (
                    "Logout",
                    EventCodec[ShowcaseEvent]
                      .encode(SelectDropdownItem("logout"))
                      .toJsonString
                  )
                )
              )
            )
          ),

          // Progress bar showcase
          card(
            title = Some("Progress Bar"),
            content = div()(
              progressBar(
                value = state.progressValue,
                label = Some("Upload Progress"),
                variant =
                  if (state.progressValue >= 100) "success"
                  else if (state.progressValue >= 75) "warning"
                  else "primary"
              ),
              div(
                style := "display: flex; gap: 10px; margin-top: 15px; justify-content: center;"
              )(
                button(
                  wvClick := EventCodec[ShowcaseEvent]
                    .encode(DecreaseProgress)
                    .toJsonString,
                  style   := "padding: 8px 16px; background: #f56565; color: white; border: none; border-radius: 6px; cursor: pointer;"
                )(Html.Text("-10%")),
                button(
                  wvClick := EventCodec[ShowcaseEvent]
                    .encode(IncreaseProgress)
                    .toJsonString,
                  style   := "padding: 8px 16px; background: #48bb78; color: white; border: none; border-radius: 6px; cursor: pointer;"
                )(Html.Text("+10%"))
              )
            )
          )
        )
      ),

      // Tab: Feedback
      tabPanel(
        tabId = "feedback",
        activeTab = state.activeTab,
        content = div()(
          h2(style := "color: #2d3748; margin-bottom: 20px;")(
            Html.Text("Feedback & Navigation")
          ),

          // Alerts showcase
          div(style := "display: grid; gap: 15px; margin-bottom: 30px;")(
            alert("This is a success message!", "success"),
            alert("Warning: Please review your settings.", "warning"),
            alert("Error: Something went wrong.", "danger"),
            alert("Info: Here's some helpful information.", "info")
          ),

          // Badges showcase
          card(
            title = Some("Badges"),
            content =
              div(style := "display: flex; flex-wrap: wrap; gap: 10px;")(
                badge("Primary", "primary"),
                Html.Text(" "),
                badge("Success", "success"),
                Html.Text(" "),
                badge("Danger", "danger"),
                Html.Text(" "),
                badge("Warning", "warning"),
                Html.Text(" "),
                badge("Info", "info"),
                Html.Text(" "),
                badge("Default", "default")
              )
          ),

          // Pagination showcase
          div(style := "margin-top: 30px;")(
            card(
              title = Some("Pagination"),
              content = div()(
                div(
                  style := "text-align: center; color: #718096; margin-bottom: 20px;"
                )(
                  Html.Text(s"Page ${state.currentPage} of ${state.totalPages}")
                ),
                pagination(
                  currentPage = state.currentPage,
                  totalPages = state.totalPages,
                  onPageChange = page => EventCodec[ShowcaseEvent].encode(ChangePage(page.toInt)).toJsonString
                )
              )
            )
          )
        )
      )
    ).render
  }

  // Helper methods

  private def extractUserValue(user: User, key: String): String = key match {
    case "id"     => user.id.toString
    case "name"   => user.name
    case "email"  => user.email
    case "role"   => user.role
    case "status" => user.status
    case _        => ""
  }

  private def renderRole(user: User): Html = {
    val variant = user.role match {
      case "Admin"     => "danger"
      case "Moderator" => "warning"
      case _           => "default"
    }
    badge(user.role, variant)
  }

  private def renderStatus(user: User): Html = {
    val variant = user.status match {
      case "Active"   => "success"
      case "Inactive" => "default"
      case _          => "default"
    }
    badge(user.status, variant)
  }

  private def sortUsers(
      users: List[User],
      column: String,
      ascending: Boolean
  ): List[User] = {
    val sorted = column match {
      case "id"     => users.sortBy(_.id)
      case "name"   => users.sortBy(_.name)
      case "email"  => users.sortBy(_.email)
      case "status" => users.sortBy(_.status)
      case _        => users
    }
    if (ascending) sorted else sorted.reverse
  }
}
