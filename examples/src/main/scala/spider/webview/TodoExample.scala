package spider.webview

import dev.alteration.branch.spider.webview.*

/** A todo list example using WebView.
  *
  * This example shows a more complete WebView application:
  * - Managing a collection of items
  * - Adding, updating, and deleting items
  * - Event handlers with parameters
  * - Form handling with wv-submit
  * - Conditional rendering
  *
  * Run this and visit: http://localhost:8080/todos
  */
object TodoExample {

  def main(args: Array[String]): Unit = {
    val server = WebViewServer()
      .withWebViewRoute("/todos", new TodoWebView())
      .withDevMode(true)
      .start(port = 8080)

    println()
    println("Visit: http://localhost:8080/todos")
    println("Press Ctrl+C to stop")
    println()

    scala.io.StdIn.readLine()
  }
}
