package spider.webview

import dev.alteration.branch.spider.webview.*

/** The simplest possible WebView example using the enhanced WebViewServer API.
  *
  * This example shows how easy it is to get started with WebView:
  *   - Define your WebView
  *   - Add it to the server with .withWebViewRoute()
  *   - Start the server
  *
  * The server automatically:
  *   - Serves HTML pages for each WebView route
  *   - Loads the webview.js client library
  *   - Sets up WebSocket connections
  *
  * Run this and visit: http://localhost:8080/counter
  */
object CounterExample {

  def main(args: Array[String]): Unit = {
    // Create a WebView server with automatic HTML serving and DevTools
    val server = WebViewServer()
      .withWebViewRoute("/counter", new CounterWebView())
      .withDevMode(true) // Enable DevTools for debugging
      .start(port = 8080)

    println()
    println("Visit: http://localhost:8080/counter")
    println("Press Ctrl+C to stop")
    println()

    // The server is now running and will block here
    // HTML page and JS are served automatically!
    scala.io.StdIn.readLine()
  }
}
