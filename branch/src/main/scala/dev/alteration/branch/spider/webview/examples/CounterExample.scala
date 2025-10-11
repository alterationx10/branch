package dev.alteration.branch.spider.webview.examples

import dev.alteration.branch.spider.webview.*

/** A complete example showing how to serve a WebView with initial params.
  *
  * This example demonstrates:
  * 1. Using the WebViewServer fluent API
  * 2. Passing initial params to the WebView
  * 3. Automatically serving HTML and JavaScript
  *
  * Run this and visit: http://localhost:8080/counter
  */
object CounterExample extends App {

  // Create a WebView server with initial params
  val server = WebViewServer()
    .withRoute(
      "/counter",
      new CounterWebView(),
      params = Map("initial" -> "10")  // Start counter at 10
    )
    .withHtmlPages()
    .withDevMode(true)  // Enable debug mode
    .start(port = 8080)

  println()
  println("Visit: http://localhost:8080/counter")
  println("Counter will start at 10 (from initial params)")
  println("Press Ctrl+C to stop")
  println()

  // Keep the server running
  scala.io.StdIn.readLine()
}
