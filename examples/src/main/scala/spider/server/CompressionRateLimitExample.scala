package spider.server

import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.server.*
import dev.alteration.branch.spider.server.Response.html
import dev.alteration.branch.spider.server.middleware.*

/** Example demonstrating compression and rate limiting middleware.
  *
  * Features:
  * - Response compression (gzip/deflate)
  * - Rate limiting with X-RateLimit headers
  * - Token bucket algorithm
  *
  * Run this and test with:
  *   curl -v -H "Accept-Encoding: gzip" http://localhost:9000/large --compressed
  *   # Make many requests to hit rate limit:
  *   for i in {1..15}; do curl -v http://localhost:9000/api/data 2>&1 | grep -E "HTTP|X-RateLimit"; done
  */
object CompressionRateLimitExample {

  import RequestHandler.given

  def main(args: Array[String]): Unit = {

    // Create server
    val server = new SpiderApp {
      override val port = 9000

      // Handler with large response (good for testing compression)
      private val largeHandler = new RequestHandler[Array[Byte], Array[Byte]] {
        def handle(request: Request[Array[Byte]]): Response[Array[Byte]] = {
          val largeText = "Lorem ipsum dolor sit amet. " * 500 // ~14KB
          val responseHtml = s"""<!DOCTYPE html>
        <html>
        <head>
          <title>Large Response</title>
          <style>
            body { font-family: sans-serif; max-width: 800px; margin: 50px auto; padding: 20px; }
          </style>
        </head>
        <body>
          <h1>Large Response (for compression testing)</h1>
          <p>This response is over 1KB and should be compressed if you send Accept-Encoding: gzip</p>
          <div>$largeText</div>
          <p>Check the Content-Encoding header in the response!</p>
        </body>
        </html>
        """
          Response(
            200,
            headers = Map("Content-Type" -> List("text/html; charset=UTF-8")),
            body = responseHtml.getBytes("UTF-8")
          )
        }
      }

      // Handler for rate limit testing
      private val apiHandler = new RequestHandler[Array[Byte], Array[Byte]] {
        def handle(request: Request[Array[Byte]]): Response[Array[Byte]] = {
          val jsonResponse = s"""{
          "status": "ok",
          "timestamp": ${System.currentTimeMillis()},
          "message": "API response"
        }"""
          Response(
            200,
            headers = Map("Content-Type" -> List("application/json")),
            body = jsonResponse.getBytes("UTF-8")
          )
        }
      }

      // Documentation handler
      private val docHandler = new RequestHandler[String, String] {
        def handle(request: Request[String]): Response[String] =
          html"""
        <!DOCTYPE html>
        <html>
        <head>
          <title>Compression & Rate Limiting Example</title>
          <style>
            body { font-family: sans-serif; max-width: 900px; margin: 50px auto; padding: 20px; }
            h1 { color: #667eea; }
            .feature { background: #f7fafc; padding: 15px; margin: 10px 0; border-left: 3px solid #48bb78; }
            code { background: #e2e8f0; padding: 2px 6px; border-radius: 3px; font-family: monospace; }
            pre { background: #2d3748; color: #e2e8f0; padding: 15px; border-radius: 5px; overflow-x: auto; }
          </style>
        </head>
        <body>
          <h1>Compression & Rate Limiting Middleware</h1>

          <div class="feature">
            <h3>🗜️ Response Compression</h3>
            <p>Automatic compression of responses based on <code>Accept-Encoding</code> header.</p>
            <p><strong>Test it:</strong></p>
            <pre>curl -v -H "Accept-Encoding: gzip" http://localhost:9000/large --compressed</pre>
            <p>Look for <code>Content-Encoding: gzip</code> in the response headers.</p>
            <ul>
              <li>Supports gzip and deflate</li>
              <li>Configurable compression level (0-9)</li>
              <li>Minimum size threshold (default 1KB)</li>
              <li>Excludes already-compressed types (images, videos, etc.)</li>
            </ul>
          </div>

          <div class="feature">
            <h3>⏱️ Rate Limiting</h3>
            <p>Token bucket rate limiting with standard headers.</p>
            <p><strong>Configuration:</strong> 10 requests per minute per IP</p>
            <p><strong>Test it:</strong></p>
            <pre>for i in {1..15}; do curl -v http://localhost:9000/api/data 2>&1 | grep -E "HTTP|X-RateLimit"; done</pre>
            <p>After 10 requests, you'll get <code>429 Too Many Requests</code></p>
            <p><strong>Headers:</strong></p>
            <ul>
              <li><code>X-RateLimit-Limit</code>: Maximum requests allowed</li>
              <li><code>X-RateLimit-Remaining</code>: Requests remaining in window</li>
              <li><code>X-RateLimit-Reset</code>: Timestamp when limit resets</li>
              <li><code>Retry-After</code>: Seconds until you can retry (on 429)</li>
            </ul>
          </div>

          <h2>Test Routes</h2>
          <ul>
            <li><a href="/large">/large</a> - Large response (test compression)</li>
            <li><a href="/api/data">/api/data</a> - API endpoint (test rate limiting)</li>
          </ul>
        </body>
        </html>
        """
      }

      // Create middleware instances
      private val compression = CompressionMiddleware(
        CompressionConfig(
          level = 6,           // Medium compression
          minSize = 1024       // Only compress responses > 1KB
        )
      )

      private val rateLimiter: RateLimitMiddleware[Array[Byte], Array[Byte]] =
        RateLimitMiddleware.perIp(
          maxRequests = 10,       // 10 requests
          windowMillis = 60000    // per minute
        )

      // Router with middleware applied
      override val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
        case (HttpMethod.GET, Nil) =>
          // Home page - no middleware
          docHandler

        case (HttpMethod.GET, "large" :: Nil) =>
          // Large response - compression only
          compression.apply(largeHandler)(using summon, summon)

        case (HttpMethod.GET, "api" :: "data" :: Nil) =>
          // API endpoint - rate limiting only (compression would interfere with headers)
          rateLimiter.apply(apiHandler)(using summon, summon)
      }
    }

    println()
    println("=" * 70)
    println("Compression & Rate Limiting Example")
    println("=" * 70)
    println()
    println("Server started on port 9000")
    println()
    println("🗜️  Compression: Enabled (gzip/deflate, level 6, min 1KB)")
    println("⏱️  Rate Limit: 10 requests/minute per IP (token bucket)")
    println()
    println("Visit:")
    println("  - http://localhost:9000/ (documentation)")
    println("  - http://localhost:9000/large (test compression)")
    println("  - http://localhost:9000/api/data (test rate limiting)")
    println()
    println("Test compression:")
    println("  curl -v -H 'Accept-Encoding: gzip' http://localhost:9000/large --compressed")
    println()
    println("Test rate limiting:")
    println("  for i in {1..15}; do")
    println("    curl -s -w '\\nHTTP: %{http_code}' http://localhost:9000/api/data")
    println("    echo")
    println("  done")
    println()
    println("Press Ctrl+C to stop")
    println("=" * 70)
    println()

    server.main(Array.empty)
  }
}
