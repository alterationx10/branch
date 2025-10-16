package spider.server

import dev.alteration.branch.spider.server.{
  SocketSpiderApp,
  ServerConfig,
  RequestHandler,
  Request,
  Response
}
import dev.alteration.branch.spider.HttpMethod
import dev.alteration.branch.spider.server.RequestHandler.given
import dev.alteration.branch.spider.server.Response.*

/** Example application demonstrating server hardening with strict limits.
  *
  * This server uses ServerConfig.strict with tight limits:
  *   - Request body: max 1MB
  *   - Request line: max 4KB
  *   - Headers: max 50 headers, 4KB per header, 32KB total
  *   - Socket timeout: 10 seconds
  *   - Request timeout: 30 seconds
  *
  * Test cases to verify limits:
  *
  * 1. Normal request (should work):
  *    curl http://localhost:9000/test
  *
  * 2. Large body (should fail with 413):
  *    dd if=/dev/zero bs=1024 count=2000 | curl -X POST --data-binary @- http://localhost:9000/upload
  *
  * 3. Too many headers (should fail with 413):
  *    for i in {1..60}; do echo "-H \"X-Custom-$i: value\""; done | xargs curl http://localhost:9000/test
  *
  * 4. Long URL (should fail with 413):
  *    curl http://localhost:9000/$(python3 -c "print('a'*5000)")
  *
  * 5. Chunked encoding (should work if enabled):
  *    echo "Hello World" | curl -X POST -H "Transfer-Encoding: chunked" --data-binary @- http://localhost:9000/upload
  */
object HardenedServerExample extends SocketSpiderApp {

  // Use strict configuration
  override val config: ServerConfig = ServerConfig.strict

  // Test handler
  val testHandler = new RequestHandler[Array[Byte], String] {
    override def handle(request: Request[Array[Byte]]): Response[String] = {
      json"""
      {
        "message": "Request accepted",
        "method": "${request.uri.toString}",
        "headers": ${request.headers.size},
        "bodySize": ${request.body.length}
      }
      """
    }
  }

  // Upload handler (accepts POST with body)
  val uploadHandler = new RequestHandler[Array[Byte], String] {
    override def handle(request: Request[Array[Byte]]): Response[String] = {
      val bodySize = request.body.length
      val preview = if (bodySize > 0) {
        new String(request.body.take(50), "UTF-8") + (if (bodySize > 50)
                                                         "..."
                                                       else "")
      } else {
        "(empty)"
      }

      json"""
      {
        "message": "Upload accepted",
        "bodySize": $bodySize,
        "preview": "$preview",
        "limits": {
          "maxBody": "${config.maxRequestBodySize.getOrElse("unlimited")}",
          "maxHeaders": ${config.maxHeaderCount},
          "socketTimeout": "${config.socketTimeout}ms"
        }
      }
      """
    }
  }

  // Info handler (shows current config)
  val infoHandler = new RequestHandler[Array[Byte], String] {
    override def handle(request: Request[Array[Byte]]): Response[String] = {
      html"""
      <!DOCTYPE html>
      <html>
      <head>
        <title>Hardened Server - Limits</title>
        <style>
          body { font-family: system-ui; max-width: 800px; margin: 40px auto; padding: 0 20px; }
          h1 { color: #d32f2f; }
          .limit { background: #ffebee; padding: 10px; margin: 10px 0; border-left: 4px solid #d32f2f; }
          code { background: #f5f5f5; padding: 2px 6px; border-radius: 3px; }
        </style>
      </head>
      <body>
        <h1>Hardened Server Configuration</h1>
        <p>This server uses <code>ServerConfig.strict</code> with tight limits for security.</p>

        <h2>Current Limits</h2>
        <div class="limit">
          <strong>Max Request Body:</strong> ${config.maxRequestBodySize
          .map(s => s"${s / 1024}KB")
          .getOrElse("unlimited")}
        </div>
        <div class="limit">
          <strong>Max Request Line:</strong> ${config.maxRequestLineLength} bytes
        </div>
        <div class="limit">
          <strong>Max Headers:</strong> ${config.maxHeaderCount}
        </div>
        <div class="limit">
          <strong>Max Header Size:</strong> ${config.maxHeaderSize} bytes
        </div>
        <div class="limit">
          <strong>Max Total Headers Size:</strong> ${config.maxTotalHeadersSize} bytes
        </div>
        <div class="limit">
          <strong>Socket Timeout:</strong> ${config.socketTimeout}ms
        </div>
        <div class="limit">
          <strong>Request Timeout:</strong> ${config.requestTimeout}ms
        </div>
        <div class="limit">
          <strong>Chunked Encoding:</strong> ${if (config.enableChunkedEncoding)
          "enabled"
        else "disabled"}
        </div>
        <div class="limit">
          <strong>Keep-Alive:</strong> ${if (config.enableKeepAlive) "enabled"
        else "disabled"}
        </div>
        <div class="limit">
          <strong>Max Keep-Alive Requests:</strong> ${config.maxKeepAliveRequests
          .map(_.toString)
          .getOrElse("unlimited")}
        </div>

        <h2>Test Commands</h2>
        <p>Try these curl commands to test the limits:</p>
        <pre><code># Normal request (should work)
curl http://localhost:9000/test

# Large body (should fail with 413)
dd if=/dev/zero bs=1024 count=2000 | curl -X POST --data-binary @- http://localhost:9000/upload

# Many headers (should fail with 413)
# (Generate 60 headers, limit is 50)
for i in {1..60}; do echo "-H \\"X-Custom-$$i: value\\""; done

# Long URL (should fail with 413)
# curl "http://localhost:9000/aaaaaaa..." (with 5000 'a's)

# Chunked encoding (should work)
echo "Hello World" | curl -X POST -H "Transfer-Encoding: chunked" --data-binary @- http://localhost:9000/upload
</code></pre>
      </body>
      </html>
      """
    }
  }

  // HTTP routes
  override val router
      : PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
    case (HttpMethod.GET, Nil)                      => infoHandler
    case (HttpMethod.GET, "test" :: Nil)            => testHandler
    case (HttpMethod.POST, "upload" :: Nil)         => uploadHandler
    case (HttpMethod.GET, path :: Nil) if path.startsWith("a") =>
      testHandler // Catch long URLs
  }
}
