package spider.server

import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.server.*
import dev.alteration.branch.spider.server.Response.{html, json}
import dev.alteration.branch.spider.server.RoutingHelpers.*

/** Example demonstrating enhanced routing features.
  *
  * Features shown:
  * - Path parameter extraction (Int, UUID, Long, etc.)
  * - Query parameter parsing
  * - Regex-based matching
  * - Route prefixing
  * - Method-specific routing helpers
  *
  * Run this and try:
  *   - http://localhost:9000/users/123
  *   - http://localhost:9000/posts/550e8400-e29b-41d4-a716-446655440000
  *   - http://localhost:9000/articles/1234567890
  *   - http://localhost:9000/prices/19.99
  *   - http://localhost:9000/search?q=scala&limit=10
  *   - http://localhost:9000/api/v1/status
  */
object EnhancedRoutingExample {

  import RequestHandler.given

  def main(args: Array[String]): Unit = {

    // Handler that extracts integer user ID
    val userHandler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        // Extract ID from path (already validated as Int by router)
        val userId = request.uri.getPath.split("/").last
        json"""
        {
          "user_id": $userId,
          "name": "User $userId",
          "email": "user$userId@example.com"
        }
        """
      }
    }

    // Handler that extracts UUID
    val postHandler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        val postId = request.uri.getPath.split("/").last
        json"""
        {
          "post_id": "$postId",
          "title": "Post with UUID",
          "content": "This post has a valid UUID"
        }
        """
      }
    }

    // Handler with query parameters
    val searchHandler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        val queryString = Option(request.uri.getQuery).getOrElse("")
        val params = QueryParams.parse(queryString)
        val query = QueryParams.get(params, "q").getOrElse("*")
        val limit = QueryParams.get(params, "limit").flatMap(_.toIntOption).getOrElse(10)
        val tags = QueryParams.getAll(params, "tag")

        json"""
        {
          "query": "$query",
          "limit": $limit,
          "tags": [${tags.map(t => s""""$t"""").mkString(", ")}],
          "results": []
        }
        """
      }
    }

    // Handler for prices with decimal values
    val priceHandler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        val price = request.uri.getPath.split("/").last
        json"""
        {
          "price": $price,
          "currency": "USD",
          "formatted": "$$${price}"
        }
        """
      }
    }

    // API status handler (for prefixed routes)
    val apiStatusHandler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] =
        json"""
        {
          "api_version": "v1",
          "status": "ok"
        }
        """
    }

    // Home handler with route documentation
    val homeHandler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] =
        html"""
        <!DOCTYPE html>
        <html>
        <head>
          <title>Enhanced Routing Example</title>
          <style>
            body { font-family: sans-serif; max-width: 900px; margin: 50px auto; padding: 20px; }
            h1 { color: #667eea; }
            .route { background: #f7fafc; padding: 15px; margin: 10px 0; border-left: 3px solid #667eea; }
            .route code { background: #e2e8f0; padding: 2px 6px; border-radius: 3px; }
            .param { color: #805ad5; font-weight: bold; }
          </style>
        </head>
        <body>
          <h1>Enhanced Routing Features</h1>
          <p>Try these routes to see path parameter extraction in action:</p>

          <div class="route">
            <h3>Integer Parameter</h3>
            <code>GET /users/<span class="param">:id</span></code>
            <p>Example: <a href="/users/123">/users/123</a></p>
          </div>

          <div class="route">
            <h3>UUID Parameter</h3>
            <code>GET /posts/<span class="param">:uuid</span></code>
            <p>Example: <a href="/posts/550e8400-e29b-41d4-a716-446655440000">/posts/550e8400-e29b-41d4-a716-446655440000</a></p>
          </div>

          <div class="route">
            <h3>Long Parameter</h3>
            <code>GET /articles/<span class="param">:timestamp</span></code>
            <p>Example: <a href="/articles/1234567890">/articles/1234567890</a></p>
          </div>

          <div class="route">
            <h3>Double Parameter</h3>
            <code>GET /prices/<span class="param">:amount</span></code>
            <p>Example: <a href="/prices/19.99">/prices/19.99</a></p>
          </div>

          <div class="route">
            <h3>Query Parameters</h3>
            <code>GET /search?q=<span class="param">query</span>&limit=<span class="param">10</span>&tag=<span class="param">scala</span></code>
            <p>Example: <a href="/search?q=scala&limit=5&tag=web&tag=http">/search?q=scala&limit=5&tag=web&tag=http</a></p>
          </div>

          <div class="route">
            <h3>Prefixed Routes</h3>
            <code>GET /api/v1/status</code>
            <p>Example: <a href="/api/v1/status">/api/v1/status</a></p>
          </div>
        </body>
        </html>
        """
    }

    // Create API router with prefix
    val apiRouter = Routes.withPrefix("api" :: "v1" :: Nil) {
      case (HttpMethod.GET, "status" :: Nil) => apiStatusHandler
    }

    // Main router using enhanced routing features
    val mainRouter: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
      // Home route
      case (HttpMethod.GET, Nil) => homeHandler

      // Path parameter extractors
      case (HttpMethod.GET, "users" :: IntParam(id) :: Nil) =>
        userHandler

      case (HttpMethod.GET, "posts" :: UuidParam(uuid) :: Nil) =>
        postHandler

      case (HttpMethod.GET, "articles" :: LongParam(timestamp) :: Nil) =>
        new RequestHandler[String, String] {
          def handle(request: Request[String]): Response[String] =
            json"""{"article_id": $timestamp, "timestamp": $timestamp}"""
        }

      case (HttpMethod.GET, "prices" :: DoubleParam(amount) :: Nil) =>
        priceHandler

      case (HttpMethod.GET, "search" :: Nil) =>
        searchHandler
    }

    // Combine all routers
    val combinedRouter = Routes.combine(mainRouter, apiRouter)

    // Create and start server
    val server = new SpiderApp {
      override val port = 9000
      override val router = combinedRouter
    }

    println()
    println("Enhanced Routing Example Server started on port 9000")
    println("Visit:")
    println("  - http://localhost:9000/")
    println("  - http://localhost:9000/users/123")
    println("  - http://localhost:9000/posts/550e8400-e29b-41d4-a716-446655440000")
    println("  - http://localhost:9000/articles/1234567890")
    println("  - http://localhost:9000/prices/19.99")
    println("  - http://localhost:9000/search?q=scala&limit=10&tag=web")
    println("  - http://localhost:9000/api/v1/status")
    println()
    println("Press Ctrl+C to stop")
    println()

    server.main(Array.empty)
  }
}
