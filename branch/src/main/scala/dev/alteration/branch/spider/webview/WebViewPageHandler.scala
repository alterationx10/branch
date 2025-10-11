package dev.alteration.branch.spider.webview

import dev.alteration.branch.spider.http.{HttpHandler, HttpResponse}
import java.net.Socket

/** An HTTP handler that serves the initial HTML page for a WebView.
  *
  * This generates an HTML page that:
  * 1. Loads the WebView client JavaScript
  * 2. Provides a div for the WebView to render into
  * 3. Connects to the WebSocket endpoint
  *
  * Example:
  * {{{
  * val pageHandler = WebViewPageHandler(
  *   wsUrl = "ws://localhost:8080/counter",
  *   title = "Counter Demo",
  *   jsPath = "/js/webview.js"
  * )
  * }}}
  *
  * @param wsUrl
  *   The WebSocket URL to connect to (e.g., "ws://localhost:8080/counter")
  * @param title
  *   The page title
  * @param jsPath
  *   The path to the webview.js file (relative to server root)
  * @param rootId
  *   The ID of the root div for rendering (default: "root")
  * @param debug
  *   Enable debug mode in the client (default: false)
  */
class WebViewPageHandler(
    wsUrl: String,
    title: String = "Branch WebView",
    jsPath: String = "/js/webview.js",
    rootId: String = "root",
    debug: Boolean = false
) extends HttpHandler {

  override def handleGet(
      path: String,
      headers: Map[String, List[String]],
      socket: Socket
  ): Unit = {
    val html = generateHtml()
    HttpResponse.ok(socket, html)
  }

  private def generateHtml(): String = {
    s"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>$title</title>
  <style>
    body {
      margin: 0;
      padding: 0;
      background: #edf2f7;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    }
    #connection-status {
      position: fixed;
      top: 10px;
      right: 10px;
      padding: 8px 16px;
      border-radius: 6px;
      font-size: 0.875rem;
      font-weight: 500;
      z-index: 1000;
      transition: all 0.3s ease;
    }
    #connection-status.connected {
      background: #48bb78;
      color: white;
    }
    #connection-status.disconnected {
      background: #f56565;
      color: white;
    }
    #connection-status.connecting {
      background: #ed8936;
      color: white;
    }
    #loading {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100vh;
      font-size: 1.5rem;
      color: #718096;
    }
    #$rootId {
      min-height: 100vh;
    }
  </style>
</head>
<body>
  <div id="connection-status" class="connecting">Connecting...</div>
  <div id="loading">Loading WebView...</div>
  <div id="$rootId"></div>

  <script src="$jsPath"></script>
  <script>
    // Create the WebView connection
    const webview = new BranchWebView('$wsUrl', {
      rootSelector: '#$rootId',
      debug: $debug
    });

    // Update connection status indicator
    const statusEl = document.getElementById('connection-status');
    const loadingEl = document.getElementById('loading');

    document.addEventListener('webview:connected', () => {
      statusEl.textContent = 'Connected';
      statusEl.className = 'connected';
      loadingEl.style.display = 'none';
    });

    document.addEventListener('webview:disconnected', () => {
      statusEl.textContent = 'Disconnected';
      statusEl.className = 'disconnected';
      loadingEl.style.display = 'flex';
    });

    document.addEventListener('webview:error', (e) => {
      console.error('WebView error:', e.detail);
    });

    document.addEventListener('webview:updated', (e) => {
      console.log('WebView updated:', e.detail);
    });
  </script>
</body>
</html>
"""
  }
}

object WebViewPageHandler {

  /** Create a WebView page handler.
    *
    * @param wsUrl
    *   The WebSocket URL to connect to
    * @param title
    *   The page title (default: "Branch WebView")
    * @param jsPath
    *   The path to webview.js (default: "/js/webview.js")
    * @param rootId
    *   The root div ID (default: "root")
    * @param debug
    *   Enable debug mode (default: false)
    * @return
    *   A new WebViewPageHandler
    */
  def apply(
      wsUrl: String,
      title: String = "Branch WebView",
      jsPath: String = "/js/webview.js",
      rootId: String = "root",
      debug: Boolean = false
  ): WebViewPageHandler = {
    new WebViewPageHandler(wsUrl, title, jsPath, rootId, debug)
  }
}
