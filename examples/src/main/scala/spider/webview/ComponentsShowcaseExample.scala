package spider.webview

import dev.alteration.branch.spider.webview.*

/** A comprehensive showcase of all Branch WebView advanced components.
  *
  * This example demonstrates:
  *   - Data tables with sortable columns
  *   - Modal dialogs with custom content
  *   - Dropdown menus
  *   - Tab navigation with multiple panels
  *   - Accordion components with collapsible sections
  *   - Cards with different variants (default, success, warning, danger)
  *   - Badges with various color schemes
  *   - Alert notifications (success, warning, danger, info)
  *   - Progress bars with dynamic updates
  *   - Pagination controls
  *
  * Run this and visit: http://localhost:8080/components
  */
object ComponentsShowcaseExample {

  def main(args: Array[String]): Unit = {
    val server = WebViewServer()
      .withRoute("/components", new ComponentsShowcaseWebView())
      .withHtmlPages()
      .withDevMode(true)
      .start(port = 8080)

    println()
    println("=" * 60)
    println("  Branch Components Showcase")
    println("=" * 60)
    println()
    println("  Visit: http://localhost:8080/components")
    println()
    println("  This showcase demonstrates all advanced UI components:")
    println("  • Data tables with sorting")
    println("  • Modal dialogs")
    println("  • Dropdown menus")
    println("  • Tab navigation")
    println("  • Accordion panels")
    println("  • Cards and badges")
    println("  • Alert notifications")
    println("  • Progress bars")
    println("  • Pagination controls")
    println()
    println("  Press Ctrl+C to stop")
    println("=" * 60)
    println()

    scala.io.StdIn.readLine()
  }
}
