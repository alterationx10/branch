---
title: Spider Middleware
description: Request/Response middleware for cross-cutting concerns
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-01-25T04:38:00Z
tags:
  - http
  - middleware
  - server
---

# Middleware

Middleware allows you to wrap request handlers with cross-cutting concerns like logging, authentication, CORS, CSRF protection, sessions, and more.

## Core Concept

Middleware can pre-process requests (before the handler) and post-process responses (after the handler):

```scala
trait Middleware[I, O] {
  // Pre-process request - can short-circuit
  def preProcess(request: Request[I]): MiddlewareResult[Response[O], Request[I]]

  // Post-process response
  def postProcess(request: Request[I], response: Response[O]): Response[O]
}
```

## Basic Usage

Apply middleware to handlers using `withMiddleware`:

```scala
import dev.alteration.branch.spider.server.middleware.*

val handler = MyHandler().withMiddleware(LoggingMiddleware())
```

## Chaining Middleware

Chain multiple middleware together:

```scala
val handler = MyHandler()
  .withMiddleware(LoggingMiddleware())
  .withMiddleware(CorsMiddleware.permissive)
  .withMiddleware(SessionMiddleware.default)
```

Or use `chain` to combine them:

```scala
val middleware = Middleware.chain(
  LoggingMiddleware(),
  CorsMiddleware.permissive,
  SessionMiddleware.default
)

val handler = MyHandler().withMiddleware(middleware)
```

## Built-in Middleware

### LoggingMiddleware

Logs request and response information:

```scala
import java.util.logging.Logger

given logger: Logger = Logger.getLogger("MyApp")

val handler = MyHandler().withMiddleware(LoggingMiddleware())
```

Logs:
- Incoming request method and path
- Response status code
- Request processing time

### RequestIdMiddleware

Adds a unique request ID to each request:

```scala
val handler = MyHandler().withMiddleware(RequestIdMiddleware())
```

The request ID is:
- Generated as a UUID for each request
- Added to the response as `X-Request-Id` header
- Accessible in the handler via thread-local context

### CorsMiddleware

Handles Cross-Origin Resource Sharing (CORS):

```scala
import dev.alteration.branch.spider.server.middleware.*

// Permissive (allows all origins) - good for development
val handler = MyHandler().withMiddleware(CorsMiddleware.permissive)

// Restrictive (specify allowed origins)
val corsConfig = CorsConfig.restrictive
  .withOrigins("https://example.com", "https://app.example.com")
  .withMethods(HttpMethod.GET, HttpMethod.POST)
  .withHeaders("Content-Type", "Authorization")
  .withCredentials(true)
  .withMaxAge(3600)

val handler = MyHandler().withMiddleware(CorsMiddleware(corsConfig))
```

Features:
- Handles preflight OPTIONS requests
- Validates origins, methods, and headers
- Supports credentials
- Configurable max age for preflight caching

### SessionMiddleware

Manages user sessions with cookie-based storage:

```scala
import dev.alteration.branch.spider.server.middleware.*

// Default configuration
val handler = MyHandler().withMiddleware(SessionMiddleware.default)

// Custom configuration
val sessionConfig = SessionConfig(
  cookieName = "SESSION",
  maxAge = 3600,        // 1 hour
  secure = true,        // HTTPS only
  httpOnly = true,      // Not accessible via JavaScript
  sameSite = Cookie.SameSite.Strict,
  slidingExpiration = true  // Extend session on each request
)

val store = InMemorySessionStore()
val handler = MyHandler().withMiddleware(
  SessionMiddleware(sessionConfig, store)
)
```

Use sessions in your handlers:

```scala
import dev.alteration.branch.spider.server.middleware.SessionExtensions.*

case class MyHandler() extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    // Get session value
    val username = request.sessionGet("username")

    // Set session value
    request.sessionSet("username", "alice")

    // Remove session value
    request.sessionRemove("username")

    // Clear all session data
    request.sessionClear()

    // Destroy session completely
    request.sessionDestroy()

    // Regenerate session ID (after login)
    request.sessionRegenerateId()

    Response(200, s"Hello, ${username.getOrElse("guest")}!")
  }
}
```

### CsrfMiddleware

Protects against Cross-Site Request Forgery attacks:

```scala
import dev.alteration.branch.spider.server.middleware.*

// Default configuration
val handler = MyHandler().withMiddleware(CsrfMiddleware.default)

// Custom configuration
val csrfConfig = CsrfConfig.default
  .withCookieName("XSRF-TOKEN")
  .withHeaderName("X-XSRF-TOKEN")
  .withExemptPaths("/api/public/*")
  .withExemptMethods(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS)

val handler = MyHandler().withMiddleware(CsrfMiddleware(csrfConfig))
```

The middleware:
- Generates CSRF tokens automatically
- Validates tokens on non-safe HTTP methods (POST, PUT, DELETE, PATCH)
- Uses double-submit cookie pattern
- Allows exempting specific paths and methods

Access CSRF tokens in handlers:

```scala
import dev.alteration.branch.spider.server.middleware.CsrfToken.*

case class MyHandler() extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    val token = request.csrfToken(CsrfConfig.default)

    Response(200, s"""
      <form method="POST" action="/submit">
        <input type="hidden" name="_csrf" value="$token" />
        <button type="submit">Submit</button>
      </form>
    """)
  }
}
```

### CompressionMiddleware

Automatically compresses responses using gzip:

```scala
import dev.alteration.branch.spider.server.middleware.*

// Compress responses over 1KB
val handler = MyHandler().withMiddleware(
  CompressionMiddleware(minSize = 1024)
)
```

Features:
- Compresses when client supports gzip (`Accept-Encoding: gzip`)
- Only compresses responses above `minSize`
- Sets appropriate `Content-Encoding` header
- Automatically handles decompression

### RateLimitMiddleware

Limits request rate per client:

```scala
import dev.alteration.branch.spider.server.middleware.*
import scala.concurrent.duration.*

// Allow 100 requests per minute per IP
val handler = MyHandler().withMiddleware(
  RateLimitMiddleware(
    maxRequests = 100,
    windowDuration = 1.minute
  )
)

// Custom key extractor (e.g., by user ID)
val handler = MyHandler().withMiddleware(
  RateLimitMiddleware(
    maxRequests = 100,
    windowDuration = 1.minute,
    keyExtractor = request => request.sessionGet("userId").getOrElse(request.remoteAddress)
  )
)
```

When rate limit is exceeded, returns 429 Too Many Requests.

## Custom Middleware

### Simple Custom Middleware

Create middleware using factory methods:

```scala
// Pre-process only (request modification or short-circuit)
val authMiddleware = Middleware.preOnly[Unit, String] { request =>
  val authHeader = request.headers.get("Authorization").flatMap(_.headOption)

  authHeader match {
    case Some(token) if isValidToken(token) =>
      Continue(request)
    case _ =>
      Respond(Response(401, "Unauthorized"))
  }
}

// Post-process only (response modification)
val headerMiddleware = Middleware.postOnly[Unit, String] { (request, response) =>
  response
    .withHeader("X-Powered-By" -> "Spider")
    .withHeader("X-Frame-Options" -> "DENY")
}

// Both pre and post-process
val timingMiddleware = Middleware[Unit, String](
  pre = request => {
    val start = System.currentTimeMillis()
    request.attributes.put("startTime", start)
    Continue(request)
  },
  post = (request, response) => {
    val start = request.attributes.get("startTime").asInstanceOf[Long]
    val duration = System.currentTimeMillis() - start
    response.withHeader("X-Response-Time" -> s"${duration}ms")
  }
)
```

### Full Custom Middleware

Extend the `Middleware` trait for complete control:

```scala
class AuthMiddleware(validTokens: Set[String]) extends Middleware[Unit, String] {

  override def preProcess(
    request: Request[Unit]
  ): MiddlewareResult[Response[String], Request[Unit]] = {
    val token = request.headers
      .get("Authorization")
      .flatMap(_.headOption)
      .map(_.replaceFirst("Bearer ", ""))

    token match {
      case Some(t) if validTokens.contains(t) =>
        // Valid token - continue
        Continue(request)

      case _ =>
        // Invalid or missing token - short-circuit with 401
        Respond(Response(401, "Unauthorized"))
    }
  }

  override def postProcess(
    request: Request[Unit],
    response: Response[String]
  ): Response[String] = {
    // Add security headers to all responses
    response
      .withHeader("X-Content-Type-Options" -> "nosniff")
      .withHeader("X-Frame-Options" -> "DENY")
  }
}

// Usage
val handler = MyHandler().withMiddleware(
  AuthMiddleware(Set("secret-token-1", "secret-token-2"))
)
```

## Middleware Composition

Middleware supports monoid composition with the `|+|` operator:

```scala
import dev.alteration.branch.macaroni.typeclasses.syntax.*

val middleware = LoggingMiddleware() |+| CorsMiddleware.permissive |+| SessionMiddleware.default

val handler = MyHandler().withMiddleware(middleware)
```

Or use the `>>` operator for sequential chaining:

```scala
val middleware = LoggingMiddleware() >> CorsMiddleware.permissive >> SessionMiddleware.default
```

## Short-Circuiting

Middleware can short-circuit request processing by returning `Respond` instead of `Continue`:

```scala
val maintenanceMiddleware = Middleware.preOnly[Unit, String] { request =>
  if (isMaintenanceMode) {
    Respond(Response(503, "Service temporarily unavailable"))
  } else {
    Continue(request)
  }
}
```

When middleware returns `Respond`:
- The handler is NOT called
- The response goes directly to post-processing
- Subsequent middleware in the chain may still post-process the response

## Middleware Order

Middleware is applied in order:

```scala
val handler = MyHandler()
  .withMiddleware(LoggingMiddleware())      // 1st preProcess, last postProcess
  .withMiddleware(CorsMiddleware.permissive) // 2nd preProcess, 2nd-to-last postProcess
  .withMiddleware(SessionMiddleware.default) // 3rd preProcess, 1st postProcess
```

Execution order:
1. LoggingMiddleware.preProcess
2. CorsMiddleware.preProcess
3. SessionMiddleware.preProcess
4. **Handler executes**
5. SessionMiddleware.postProcess
6. CorsMiddleware.postProcess
7. LoggingMiddleware.postProcess

## Best Practices

1. **Order matters**: Place authentication/authorization middleware early in the chain
2. **Use short-circuits wisely**: Return early for unauthorized requests to avoid unnecessary processing
3. **Keep middleware focused**: Each middleware should handle one concern
4. **Compose middleware**: Build complex behavior by combining simple middleware
5. **Consider performance**: Middleware runs on every request, so keep it fast

## Example: Complete Middleware Stack

```scala
import dev.alteration.branch.spider.server.middleware.*
import scala.concurrent.duration.*

val sessionConfig = SessionConfig.default.withSecure(true)
val sessionStore = InMemorySessionStore()

val corsConfig = CorsConfig.restrictive
  .withOrigins("https://example.com")
  .withCredentials(true)

val csrfConfig = CsrfConfig.default
  .withExemptPaths("/api/public/*")

val middleware = Middleware.chain(
  LoggingMiddleware(),                               // Log all requests
  RequestIdMiddleware(),                             // Add request IDs
  CorsMiddleware(corsConfig),                        // Handle CORS
  RateLimitMiddleware(100, 1.minute),               // Rate limiting
  CompressionMiddleware(minSize = 1024),            // Compress responses
  SessionMiddleware(sessionConfig, sessionStore),   // Session management
  CsrfMiddleware(csrfConfig)                        // CSRF protection
)

val handler = MyHandler().withMiddleware(middleware)
```

## Next Steps

- Learn about [Cookies and Sessions](cookies-sessions.md)
- Explore [Request/Response Parsing](body-parsing.md)
- Return to [HTTP Server](server.md)
