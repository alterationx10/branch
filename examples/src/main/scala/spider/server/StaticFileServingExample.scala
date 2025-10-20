package spider.server

import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.server.*
import dev.alteration.branch.spider.server.Response.html
import java.nio.file.Path

/** Example demonstrating enhanced static file serving features.
  *
  * Features shown:
  * - ETag generation and If-None-Match handling (304 responses)
  * - Range request support for partial content (206 responses)
  * - Cache-Control headers for browser caching
  * - Serving from filesystem with automatic content-type detection
  *
  * To test:
  * 1. Create a test directory: mkdir -p /tmp/static-test
  * 2. Add some files:
  *    echo "Hello World" > /tmp/static-test/hello.txt
  *    echo "<h1>Test Page</h1>" > /tmp/static-test/index.html
  * 3. Run this example
  * 4. Test with curl:
  *    curl -v http://localhost:9000/hello.txt
  *    curl -v -H "If-None-Match: <etag>" http://localhost:9000/hello.txt
  *    curl -v -H "Range: bytes=0-4" http://localhost:9000/hello.txt
  */
object StaticFileServingExample {

  import RequestHandler.given

  def main(args: Array[String]): Unit = {

    // Documentation handler
    val docHandler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] =
        html"""
        <!DOCTYPE html>
        <html>
        <head>
          <title>Static File Serving Example</title>
          <style>
            body { font-family: sans-serif; max-width: 900px; margin: 50px auto; padding: 20px; }
            h1 { color: #667eea; }
            .feature { background: #f7fafc; padding: 15px; margin: 10px 0; border-left: 3px solid #48bb78; }
            code { background: #e2e8f0; padding: 2px 6px; border-radius: 3px; }
            pre { background: #2d3748; color: #e2e8f0; padding: 15px; border-radius: 5px; overflow-x: auto; }
          </style>
        </head>
        <body>
          <h1>Enhanced Static File Serving</h1>
          <p>This example demonstrates the enhanced file serving capabilities:</p>

          <div class="feature">
            <h3>🏷️ ETag Support</h3>
            <p>ETags are automatically generated for files based on their path, size, and modification time.</p>
            <pre>curl -v http://localhost:9000/static/hello.txt
# Note the ETag header in response

curl -v -H "If-None-Match: W/\"abc123...\"" http://localhost:9000/static/hello.txt
# Returns 304 Not Modified</pre>
          </div>

          <div class="feature">
            <h3>📦 Range Request Support</h3>
            <p>Perfect for video streaming and large file downloads.</p>
            <pre>curl -v -H "Range: bytes=0-99" http://localhost:9000/static/hello.txt
# Returns 206 Partial Content with Content-Range header</pre>
          </div>

          <div class="feature">
            <h3>⚡ Cache-Control Headers</h3>
            <p>Configurable caching with <code>max-age</code> parameter.</p>
            <pre># Default: max-age=3600 (1 hour)
# Custom: FileHandler(path, maxAge = 86400) // 24 hours</pre>
          </div>

          <div class="feature">
            <h3>🎯 Automatic Content-Type</h3>
            <p>MIME types are automatically detected from file extensions.</p>
          </div>

          <h2>Setup Instructions</h2>
          <pre>mkdir -p /tmp/static-test
echo "Hello World" > /tmp/static-test/hello.txt
echo "&lt;h1&gt;Test Page&lt;/h1&gt;" > /tmp/static-test/index.html
curl http://localhost:9000/static/index.html</pre>

          <h2>Test Routes</h2>
          <ul>
            <li><a href="/static/index.html">/static/index.html</a></li>
            <li><a href="/static/hello.txt">/static/hello.txt</a></li>
          </ul>
        </body>
        </html>
        """
    }

    // Static file directory
    val staticDir = Path.of("/tmp/static-test")

    // Create the static file handler with 1 hour cache
    val staticFileHandler = FileHandler(staticDir, maxAge = 3600)

    // Alternative: Longer cache for production (24 hours)
    // val staticFileHandler = FileHandler(staticDir, maxAge = 86400)

    // Router
    val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
      case (HttpMethod.GET, Nil) => docHandler
      case (HttpMethod.GET, "static" :: _) => staticFileHandler
    }

    // Create and start server
    val server = new SpiderApp {
      override val port = 9000
      override val router = router
    }

    println()
    println("=" * 60)
    println("Static File Serving Example")
    println("=" * 60)
    println()
    println("Server started on port 9000")
    println()
    println("📁 Serving files from: " + staticDir.toAbsolutePath)
    println()
    println("Setup your test files:")
    println("  mkdir -p /tmp/static-test")
    println("  echo 'Hello World' > /tmp/static-test/hello.txt")
    println("  echo '<h1>Test</h1>' > /tmp/static-test/index.html")
    println()
    println("Visit:")
    println("  - http://localhost:9000/ (documentation)")
    println("  - http://localhost:9000/static/index.html")
    println("  - http://localhost:9000/static/hello.txt")
    println()
    println("Test with curl:")
    println("  # Basic request")
    println("  curl -v http://localhost:9000/static/hello.txt")
    println()
    println("  # ETag validation (copy ETag from first response)")
    println("  curl -v -H 'If-None-Match: W/\"...\"' http://localhost:9000/static/hello.txt")
    println()
    println("  # Range request")
    println("  curl -v -H 'Range: bytes=0-4' http://localhost:9000/static/hello.txt")
    println()
    println("Press Ctrl+C to stop")
    println("=" * 60)
    println()

    server.main(Array.empty)
  }
}
