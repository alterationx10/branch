package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.server.{Request, Response}

import java.net.URI

class SessionMiddlewareSpec extends munit.FunSuite {

  override def beforeEach(context: BeforeEach): Unit = {
    // Clear session context before each test
    SessionContext.clear()
  }

  test("SessionMiddleware creates new session when none exists") {
    val store      = InMemorySessionStore()
    val config     = SessionConfig.development
    val middleware = SessionMiddleware[String, String](config, store)

    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = ""
    )

    // Create a new session in context
    val session = SessionContext.getOrCreate(config)
    session.set("user", "john")

    val response = Response(200, "OK")
    val result   = middleware.postProcess(request, response)

    // Should have Set-Cookie header
    val setCookieHeaders = result.headers.get("Set-Cookie")
    assert(setCookieHeaders.isDefined)
    assert(setCookieHeaders.get.exists(_.contains("SESSION_ID=")))

    // Session should be saved in store
    assert(store.exists(session.id))
  }

  test("SessionMiddleware loads existing session from cookie") {
    val store      = InMemorySessionStore()
    val config     = SessionConfig.development
    val middleware = SessionMiddleware[String, String](config, store)

    // Create and save a session
    val session = Session.create(Map("user" -> "john"), 3600)
    store.save(session)

    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map("Cookie" -> List(s"SESSION_ID=${session.id}")),
      body = ""
    )

    val result = middleware.preProcess(request)

    result match {
      case Continue(_) =>
        // Session should be loaded into context
        val loadedSession = SessionContext.get()
        assert(loadedSession.isDefined)
        assertEquals(loadedSession.get.id, session.id)
        assertEquals(loadedSession.get.get("user"), Some("john"))

      case Respond(_) =>
        fail("Should not respond in preProcess")
    }
  }

  test("SessionMiddleware does not load expired session") {
    val store      = InMemorySessionStore()
    val config     = SessionConfig.development
    val middleware = SessionMiddleware[String, String](config, store)

    // Create an expired session
    val session = Session(
      id = "test-id",
      data = Map("user" -> "john"),
      expiresAt = java.time.Instant.now().minusSeconds(1)
    )
    store.save(session)

    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map("Cookie" -> List(s"SESSION_ID=${session.id}")),
      body = ""
    )

    val result = middleware.preProcess(request)

    result match {
      case Continue(_) =>
        // Session should not be loaded
        assertEquals(SessionContext.get(), None)

      case Respond(_) =>
        fail("Should not respond in preProcess")
    }
  }

  test("SessionMiddleware with sliding expiration extends session") {
    val store      = InMemorySessionStore()
    val config     = SessionConfig.development.copy(slidingExpiration = true)
    val middleware = SessionMiddleware[String, String](config, store)

    // Create a session that's about to expire
    val session   = Session.create(3600)
    val oldExpiry = session.expiresAt
    store.save(session)

    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map("Cookie" -> List(s"SESSION_ID=${session.id}")),
      body = ""
    )

    // Allow some time to pass
    Thread.sleep(10)

    middleware.preProcess(request)

    // Session should be extended
    val loadedSession = SessionContext.get()
    assert(loadedSession.isDefined)
    assert(loadedSession.get.expiresAt.isAfter(oldExpiry))
  }

  test("SessionMiddleware without sliding expiration only touches session") {
    val store      = InMemorySessionStore()
    val config     = SessionConfig.development.copy(slidingExpiration = false)
    val middleware = SessionMiddleware[String, String](config, store)

    val session   = Session.create(3600)
    val oldExpiry = session.expiresAt
    val oldAccess = session.lastAccessedAt
    store.save(session)

    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map("Cookie" -> List(s"SESSION_ID=${session.id}")),
      body = ""
    )

    Thread.sleep(10)

    middleware.preProcess(request)

    val loadedSession = SessionContext.get()
    assert(loadedSession.isDefined)
    // Expiry should remain the same (within tolerance)
    assert(
      Math.abs(
        loadedSession.get.expiresAt.getEpochSecond - oldExpiry.getEpochSecond
      ) <= 1
    )
    // Last accessed should be updated
    assert(loadedSession.get.lastAccessedAt.isAfter(oldAccess))
  }

  test("SessionMiddleware sets secure cookie attributes") {
    val store      = InMemorySessionStore()
    val config     = SessionConfig.strict
    val middleware = SessionMiddleware[String, String](config, store)

    SessionContext.getOrCreate(config)

    val request  = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = ""
    )
    val response = Response(200, "OK")
    val result   = middleware.postProcess(request, response)

    val setCookieHeaders = result.headers.get("Set-Cookie")
    assert(setCookieHeaders.isDefined)

    val cookieHeader = setCookieHeaders.get.head
    assert(cookieHeader.contains("Secure"))
    assert(cookieHeader.contains("HttpOnly"))
    assert(cookieHeader.contains("SameSite=Strict"))
  }

  test("SessionContext.getOrCreate creates session when none exists") {
    val config  = SessionConfig.development
    val session = SessionContext.getOrCreate(config)

    assert(session.id.nonEmpty)
    assertEquals(SessionContext.get(), Some(session))
  }

  test("SessionContext.getOrCreate returns existing session") {
    val config   = SessionConfig.development
    val session1 = SessionContext.getOrCreate(config)
    val session2 = SessionContext.getOrCreate(config)

    assertEquals(session1.id, session2.id)
  }

  test("SessionContext.update modifies session") {
    val config = SessionConfig.development
    SessionContext.getOrCreate(config)

    SessionContext.update(_.set("user", "john"))

    val updated = SessionContext.get()
    assertEquals(updated.flatMap(_.get("user")), Some("john"))
  }

  test("SessionContext.regenerateId creates new ID") {
    val config     = SessionConfig.development
    val session    = SessionContext.getOrCreate(config).set("user", "john")
    SessionContext.set(session)
    val originalId = session.id

    SessionContext.regenerateId()

    val updated = SessionContext.get()
    assert(updated.isDefined)
    assert(updated.get.id != originalId)
    assertEquals(updated.get.get("user"), Some("john"))
  }

  test("SessionContext.destroy clears session") {
    val config = SessionConfig.development
    SessionContext.getOrCreate(config)

    assert(SessionContext.get().isDefined)

    SessionContext.destroy()

    assertEquals(SessionContext.get(), None)
  }
}
