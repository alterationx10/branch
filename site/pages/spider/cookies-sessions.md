---
title: Spider Cookies and Sessions
description: Cookie handling and session management
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-01-25T04:38:00Z
tags:
  - http
  - cookies
  - sessions
---

# Cookies and Sessions

Spider provides comprehensive support for HTTP cookies and session management.

## Cookies

### Reading Cookies

Access cookies from requests:

```scala
import dev.alteration.branch.spider.server.Cookie

case class MyHandler() extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    // Get all cookies as a map
    val cookies = Cookie.fromHeaders(request.headers)

    // Get a specific cookie
    val sessionId = cookies.get("session_id")

    // Or use the helper method
    val userId = request.cookie("user_id")

    Response(200, s"Session: ${sessionId.getOrElse("none")}")
  }
}
```

### Setting Cookies

Add cookies to responses:

```scala
import dev.alteration.branch.spider.server.Cookie
import dev.alteration.branch.spider.server.Cookie.SameSite

case class MyHandler() extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    val cookie = Cookie(
      name = "session_id",
      value = "abc123",
      path = Some("/"),
      domain = Some("example.com"),
      maxAge = Some(3600),      // 1 hour in seconds
      secure = true,             // HTTPS only
      httpOnly = true,           // Not accessible via JavaScript
      sameSite = Some(SameSite.Strict)
    )

    Response(200, "Cookie set").withCookie(cookie)
  }
}
```

### Cookie Builder Pattern

Build cookies fluently:

```scala
val cookie = Cookie("session_id", "abc123")
  .withPath("/app")
  .withDomain("example.com")
  .withMaxAge(3600)
  .withSecure
  .withHttpOnly
  .withSameSite(Cookie.SameSite.Lax)

Response(200, "OK").withCookie(cookie)
```

### SameSite Attribute

Control cross-site request behavior:

```scala
// Strict: Cookie only sent in first-party context
Cookie("id", "123").withSameSite(Cookie.SameSite.Strict)

// Lax: Cookie sent with top-level navigations (default for most browsers)
Cookie("id", "123").withSameSite(Cookie.SameSite.Lax)

// None: Cookie sent in all contexts (requires Secure flag)
Cookie("id", "123")
  .withSameSite(Cookie.SameSite.None)
  .withSecure
```

### Signed Cookies

Prevent cookie tampering with HMAC signatures:

```scala
import dev.alteration.branch.spider.server.SignedCookie

val secret = "your-secret-key"

// Sign a cookie
val signed = SignedCookie.sign("session_id", "abc123", secret)
// Returns: "abc123.signature"

// Verify and decode
val verified = SignedCookie.verify("session_id", signed, secret)
// Returns: Some("abc123") if valid, None if tampered

// Use in handlers
case class MyHandler() extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    // Set signed cookie
    val value = "user123"
    val signedValue = SignedCookie.sign("user_id", value, secret)
    val cookie = Cookie("user_id", signedValue).withHttpOnly

    // Verify signed cookie
    val cookieValue = request.cookie("user_id")
    val userId = cookieValue.flatMap(SignedCookie.verify("user_id", _, secret))

    Response(200, s"User: ${userId.getOrElse("invalid")}")
      .withCookie(cookie)
  }
}
```

### Deleting Cookies

Delete cookies by setting maxAge to 0:

```scala
val deleteCookie = Cookie("session_id", "")
  .withMaxAge(0)
  .withPath("/")

Response(200, "Logged out").withCookie(deleteCookie)
```

## Sessions

Sessions provide server-side state management using cookies to store session IDs.

### Basic Session Usage

Use `SessionMiddleware` to enable sessions:

```scala
import dev.alteration.branch.spider.server.middleware.*

// Apply session middleware
val handler = MyHandler()
  .withMiddleware(SessionMiddleware.default)
```

Access sessions in handlers:

```scala
import dev.alteration.branch.spider.server.middleware.SessionExtensions.*

case class MyHandler() extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    // Get session value
    val username = request.sessionGet("username")

    // Set session value
    request.sessionSet("username", "alice")

    // Remove session value
    request.sessionRemove("temp_data")

    // Clear all session data
    request.sessionClear()

    Response(200, s"Hello, ${username.getOrElse("guest")}!")
  }
}
```

### Session Configuration

Customize session behavior:

```scala
import dev.alteration.branch.spider.server.middleware.*

val sessionConfig = SessionConfig(
  cookieName = "SESSION",
  maxAge = 3600,                    // Session duration in seconds (1 hour)
  path = "/",
  domain = Some("example.com"),
  secure = true,                    // HTTPS only
  httpOnly = true,                  // Not accessible via JavaScript
  sameSite = Cookie.SameSite.Lax,
  slidingExpiration = true          // Extend session on each request
)

val store = InMemorySessionStore()
val middleware = SessionMiddleware(sessionConfig, store)
```

#### Preset Configurations

```scala
// Development: Less restrictive, longer sessions
SessionMiddleware.development

// Default: Balanced settings
SessionMiddleware.default

// Strict: Short sessions, strict security
SessionMiddleware.strict
```

### Session Storage

#### InMemorySessionStore

Default in-memory storage (data lost on restart):

```scala
val store = InMemorySessionStore()
```

Features:
- Fast access
- Automatic cleanup of expired sessions
- Thread-safe
- No persistence

#### Custom Session Store

Implement your own session storage:

```scala
import dev.alteration.branch.spider.server.middleware.*

class DatabaseSessionStore(db: Database) extends SessionStore {
  override def get(sessionId: String): Option[Session] = {
    db.query("SELECT * FROM sessions WHERE id = ?", sessionId)
      .map(row => Session(
        id = row.getString("id"),
        data = parseJson(row.getString("data")),
        createdAt = row.getTimestamp("created_at"),
        lastAccessedAt = row.getTimestamp("last_accessed_at"),
        expiresAt = row.getTimestamp("expires_at")
      ))
  }

  override def save(session: Session): Unit = {
    db.execute(
      "INSERT INTO sessions (id, data, created_at, last_accessed_at, expires_at) VALUES (?, ?, ?, ?, ?) ON CONFLICT (id) DO UPDATE SET ...",
      session.id, toJson(session.data), session.createdAt, session.lastAccessedAt, session.expiresAt
    )
  }

  override def delete(sessionId: String): Unit = {
    db.execute("DELETE FROM sessions WHERE id = ?", sessionId)
  }

  override def cleanup(): Unit = {
    db.execute("DELETE FROM sessions WHERE expires_at < ?", Instant.now())
  }
}
```

### Session Lifecycle

#### Creating Sessions

Sessions are created automatically when you first set a value:

```scala
// No session exists yet
request.sessionGet("key") // None

// Setting a value creates the session
request.sessionSet("username", "alice")

// Now the session exists
request.sessionGet("username") // Some("alice")
```

Or explicitly create a session:

```scala
val session = request.getOrCreateSession(sessionConfig)
```

#### Session Expiration

Sessions expire based on `maxAge`:

```scala
val config = SessionConfig(
  maxAge = 3600,           // Absolute expiration: 1 hour from creation
  slidingExpiration = false
)
```

With sliding expiration, sessions extend on each request:

```scala
val config = SessionConfig(
  maxAge = 3600,           // 1 hour from last access
  slidingExpiration = true // Reset expiration on each request
)
```

#### Destroying Sessions

Explicitly destroy a session:

```scala
request.sessionDestroy()
```

This:
- Clears all session data
- Removes the session from the store
- Deletes the session cookie

#### Session ID Regeneration

Regenerate the session ID (important after login):

```scala
// After successful login
request.sessionSet("user_id", userId)
request.sessionRegenerateId()  // New session ID, same data
```

This prevents session fixation attacks.

### Session Security

#### Best Practices

1. **Use HTTPS**: Always set `secure = true` in production
2. **HttpOnly cookies**: Set `httpOnly = true` to prevent XSS attacks
3. **SameSite attribute**: Use `Strict` or `Lax` to prevent CSRF
4. **Regenerate ID after login**: Prevent session fixation
5. **Short expiration**: Use reasonable `maxAge` values
6. **Signed cookies**: Consider using signed cookies for sensitive data

#### Secure Session Configuration

```scala
val secureConfig = SessionConfig(
  cookieName = "SESSION",
  maxAge = 900,                     // 15 minutes
  secure = true,                    // HTTPS only
  httpOnly = true,                  // XSS protection
  sameSite = Cookie.SameSite.Strict,// CSRF protection
  slidingExpiration = true
)
```

### Example: Login Flow

```scala
import dev.alteration.branch.spider.server.middleware.SessionExtensions.*

case class LoginHandler() extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    val username = request.formParam("username")
    val password = request.formParam("password")

    if (authenticate(username, password)) {
      // Set session data
      request.sessionSet("user_id", username.get)
      request.sessionSet("authenticated", "true")

      // Regenerate session ID for security
      request.sessionRegenerateId()

      Response(302, "")
        .withHeader("Location" -> "/dashboard")
    } else {
      Response(401, "Invalid credentials")
    }
  }
}

case class LogoutHandler() extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    // Destroy the session
    request.sessionDestroy()

    Response(302, "")
      .withHeader("Location" -> "/login")
  }
}

case class DashboardHandler() extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    val userId = request.sessionGet("user_id")

    userId match {
      case Some(user) =>
        Response(200, s"Welcome, $user!")
      case None =>
        Response(302, "")
          .withHeader("Location" -> "/login")
    }
  }
}
```

## Flash Messages

Flash messages are session values that persist for only one request:

```scala
// Set a flash message
request.sessionSet("flash_message", "Login successful!")

// Read and remove flash message
val flash = request.sessionGet("flash_message")
request.sessionRemove("flash_message")

Response(200, flash.getOrElse(""))
```

You can wrap this in a helper:

```scala
object FlashHelper {
  def setFlash(request: Request[?], message: String): Unit = {
    request.sessionSet("_flash", message)
  }

  def getFlash(request: Request[?]): Option[String] = {
    val flash = request.sessionGet("_flash")
    request.sessionRemove("_flash")
    flash
  }
}
```

## Next Steps

- Learn about [Middleware](/spider/middleware)
- Explore [Request/Response Parsing](/spider/body-parsing)
- Return to [HTTP Server](/spider/server)
