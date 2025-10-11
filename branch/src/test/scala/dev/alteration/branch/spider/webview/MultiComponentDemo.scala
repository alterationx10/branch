package dev.alteration.branch.spider.webview

import dev.alteration.branch.keanu.actors.ActorSystem
import dev.alteration.branch.spider.websocket.WebSocketServer

import java.nio.file.{Files, Path}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn

/** Demo showing multiple WebView components in one application.
  *
  * This demonstrates:
  *   - Multiple WebView routes (counter and todo list)
  *   - Different component architectures
  *   - How to structure a multi-page WebView application
  *
  * Routes:
  *   - ws://localhost:9001 - Counter (simple component)
  *   - ws://localhost:9002 - TodoList (complex multi-component)
  */
object MultiComponentDemo {

  def main(args: Array[String]): Unit = {
    println("=" * 70)
    println("Branch WebView - Multi-Component Demo")
    println("=" * 70)

    // Create actor system (shared across all WebViews)
    val actorSystem = ActorSystem()

    // Create handlers for different WebView components
    val counterHandler = WebViewHandler[CounterState](
      actorSystem = actorSystem,
      webViewFactory = () => new CounterWebView(),
      params = Map.empty,
      session = Session()
    )

    val todoListHandler = WebViewHandler[TodoListState](
      actorSystem = actorSystem,
      webViewFactory = () => new TodoListWebView(),
      params = Map.empty,
      session = Session()
    )

    // Start WebSocket servers on different ports
    val counterServer = WebSocketServer.start(9001, counterHandler)
    val todoServer = WebSocketServer.start(9002, todoListHandler)

    println()
    println("WebSocket servers started:")
    println("  - Counter:  ws://localhost:9001")
    println("  - TodoList: ws://localhost:9002")
    println()
    println("=" * 70)
    println()
    println("Demo Pages:")
    println("-" * 70)
    println()

    println("1. COUNTER (Simple Component)")
    println("   Save this HTML as 'counter.html':")
    println()
    Files.write(Path.of("/tmp/counter.html"), counterHtml.getBytes())
//    printHtmlFile("counter.html", counterHtml)
    println()

    println("2. TODO LIST (Multi-Component)")
    println("   Save this HTML as 'todolist.html':")
    println()
    Files.write(Path.of("/tmp/todolist.html"), todoListHtml.getBytes())
//    printHtmlFile("todolist.html", todoListHtml)
    println()

    println("3. INDEX (Navigation)")
    println("   Save this HTML as 'index.html':")
    println()
    Files.write(Path.of("/tmp/index.html"), indexHtml.getBytes())
//    printHtmlFile("index.html", indexHtml)

    Files.write(Path.of("/tmp/webview-client.js"), clientJavaScript.getBytes)
    
    println()
    println("=" * 70)
    println("Press ENTER to stop all servers...")
    println()

    // Wait for user input
    StdIn.readLine()

    // Cleanup
    println("Shutting down...")
    counterServer.stop()
    todoServer.stop()
    actorSystem.shutdownAwait()
    println("Goodbye!")
  }

  private def printHtmlFile(filename: String, content: String): Unit = {
    println(s"   File: $filename")
    println("   " + "-" * 66)
    // Just show first and last few lines to keep output manageable
    val lines = content.split("\n")
    if (lines.length > 20) {
      lines.take(10).foreach(line => println(s"   $line"))
      println(s"   ... (${lines.length - 20} more lines) ...")
      lines.takeRight(10).foreach(line => println(s"   $line"))
    } else {
      lines.foreach(line => println(s"   $line"))
    }
  }

  /** Counter HTML page */
  val counterHtml: String = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Branch WebView - Counter</title>
</head>
<body>
  <div id="root">
    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 50px auto; padding: 20px; text-align: center;">
      <h1>Connecting to Counter...</h1>
      <p>Please wait while we connect to the WebView server.</p>
    </div>
  </div>

  <script src="webview-client.js"></script>
  <script>
    // Initialize Counter WebView connection
    const webview = new BranchWebView('ws://localhost:9001', {
      debug: true
    });
  </script>
</body>
</html>"""

  /** TodoList HTML page */
  val todoListHtml: String = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Branch WebView - TodoList</title>
</head>
<body>
  <div id="root">
    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 50px auto; padding: 20px; text-align: center;">
      <h1>Connecting to TodoList...</h1>
      <p>Please wait while we connect to the WebView server.</p>
    </div>
  </div>

  <script src="webview-client.js"></script>
  <script>
    // Initialize TodoList WebView connection
    const webview = new BranchWebView('ws://localhost:9002', {
      debug: true
    });
  </script>
</body>
</html>"""

  /** Index page with navigation */
  val indexHtml: String = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Branch WebView - Multi-Component Demo</title>
  <style>
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      margin: 0;
      padding: 20px;
      min-height: 100vh;
    }
    .container {
      max-width: 800px;
      margin: 50px auto;
      background: white;
      border-radius: 12px;
      padding: 40px;
      box-shadow: 0 20px 60px rgba(0,0,0,0.3);
    }
    h1 {
      color: #667eea;
      margin-top: 0;
    }
    .subtitle {
      color: #666;
      margin-bottom: 40px;
    }
    .components {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 20px;
      margin-bottom: 40px;
    }
    .component-card {
      border: 2px solid #e0e0e0;
      border-radius: 8px;
      padding: 24px;
      transition: all 0.3s;
      text-decoration: none;
      color: inherit;
      display: block;
    }
    .component-card:hover {
      border-color: #667eea;
      transform: translateY(-4px);
      box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);
    }
    .component-icon {
      font-size: 48px;
      margin-bottom: 16px;
    }
    .component-title {
      font-size: 24px;
      font-weight: bold;
      margin-bottom: 8px;
      color: #333;
    }
    .component-desc {
      color: #666;
      font-size: 14px;
      line-height: 1.5;
    }
    .features {
      background: #f8f9fa;
      border-radius: 8px;
      padding: 24px;
      margin-top: 40px;
    }
    .features h2 {
      margin-top: 0;
      color: #333;
    }
    .features ul {
      margin: 0;
      padding-left: 20px;
    }
    .features li {
      margin: 8px 0;
      color: #666;
    }
  </style>
</head>
<body>
  <div class="container">
    <h1>Branch WebView Demo</h1>
    <p class="subtitle">A LiveView-like framework for Scala</p>

    <div class="components">
      <a href="counter.html" class="component-card">
        <div class="component-icon">🔢</div>
        <div class="component-title">Counter</div>
        <div class="component-desc">
          Simple single-component example demonstrating basic state management and event handling.
        </div>
      </a>

      <a href="todolist.html" class="component-card">
        <div class="component-icon">✅</div>
        <div class="component-title">TodoList</div>
        <div class="component-desc">
          Complex multi-component application with nested state, filters, and real-time statistics.
        </div>
      </a>
    </div>

    <div class="features">
      <h2>Key Features</h2>
      <ul>
        <li><strong>Component Architecture:</strong> Build complex UIs with multiple interactive components</li>
        <li><strong>Server-Side State:</strong> All state lives on the server, keeping client simple</li>
        <li><strong>Real-Time Updates:</strong> WebSocket-based instant UI updates</li>
        <li><strong>Type-Safe:</strong> Full Scala type safety from server to client</li>
        <li><strong>Zero Dependencies:</strong> Pure Scala with minimal JavaScript</li>
        <li><strong>Actor-Based:</strong> Leverages Keanu actors for concurrency</li>
      </ul>
    </div>

    <div class="features" style="margin-top: 20px;">
      <h2>Component Patterns Demonstrated</h2>
      <ul>
        <li><strong>Single Component (Counter):</strong> Simple state with direct event handling</li>
        <li><strong>Multi-Component (TodoList):</strong> Complex composition with:
          <ul>
            <li>Header component (input handling)</li>
            <li>Stats component (derived state)</li>
            <li>Filter component (view filtering)</li>
            <li>List component (collection rendering)</li>
            <li>Footer component (batch operations)</li>
          </ul>
        </li>
      </ul>
    </div>
  </div>
</body>
</html>"""

  /** Standalone WebView client (to save as webview-client.js) */
  val clientJavaScript: String = scala.io.Source
    .fromResource("spider/webview/webview.js")
    .mkString
}
