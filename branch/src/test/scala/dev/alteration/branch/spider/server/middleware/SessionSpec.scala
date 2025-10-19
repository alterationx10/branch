package dev.alteration.branch.spider.server.middleware

import java.time.Instant

class SessionSpec extends munit.FunSuite {

  test("Session.create creates a session with the correct duration") {
    val durationSeconds = 3600L
    val session         = Session.create(durationSeconds)

    assert(session.id.nonEmpty)
    assert(session.data.isEmpty)
    assert(!session.isExpired)

    val expectedExpiry = Instant.now().plusSeconds(durationSeconds)
    // Allow 1 second tolerance for test execution time
    assert(
      Math.abs(
        session.expiresAt.getEpochSecond - expectedExpiry.getEpochSecond
      ) <= 1
    )
  }

  test("Session.create with data initializes session data") {
    val data            = Map("user" -> "john", "role" -> "admin")
    val durationSeconds = 3600L
    val session         = Session.create(data, durationSeconds)

    assertEquals(session.data, data)
    assert(session.id.nonEmpty)
  }

  test("Session.generateId creates unique IDs") {
    val id1 = Session.generateId()
    val id2 = Session.generateId()

    assert(id1.nonEmpty)
    assert(id2.nonEmpty)
    assert(id1 != id2)
  }

  test("Session.isExpired returns true for expired sessions") {
    val expiredSession = Session(
      id = "test-id",
      expiresAt = Instant.now().minusSeconds(1)
    )

    assert(expiredSession.isExpired)
  }

  test("Session.isExpired returns false for valid sessions") {
    val validSession = Session(
      id = "test-id",
      expiresAt = Instant.now().plusSeconds(3600)
    )

    assert(!validSession.isExpired)
  }

  test("Session.touch updates lastAccessedAt") {
    val session        = Session.create(3600)
    val originalAccess = session.lastAccessedAt

    // Sleep a bit to ensure time difference
    Thread.sleep(10)

    val touched = session.touch

    assert(touched.lastAccessedAt.isAfter(originalAccess))
  }

  test("Session.get retrieves values from data") {
    val session = Session.create(Map("key1" -> "value1"), 3600)

    assertEquals(session.get("key1"), Some("value1"))
    assertEquals(session.get("key2"), None)
  }

  test("Session.set adds values to data") {
    val session  = Session.create(3600)
    val updated  = session.set("user", "john")
    val updated2 = updated.set("role", "admin")

    assertEquals(updated.get("user"), Some("john"))
    assertEquals(updated2.get("role"), Some("admin"))
  }

  test("Session.remove deletes values from data") {
    val session =
      Session.create(Map("key1" -> "value1", "key2" -> "value2"), 3600)
    val updated = session.remove("key1")

    assertEquals(updated.get("key1"), None)
    assertEquals(updated.get("key2"), Some("value2"))
  }

  test("Session.clear removes all data") {
    val session =
      Session.create(Map("key1" -> "value1", "key2" -> "value2"), 3600)
    val cleared = session.clear

    assert(cleared.data.isEmpty)
    assertEquals(cleared.id, session.id)
  }

  test("Session.contains checks for key existence") {
    val session = Session.create(Map("key1" -> "value1"), 3600)

    assert(session.contains("key1"))
    assert(!session.contains("key2"))
  }

  test("Session.regenerateId creates new ID but preserves data") {
    val session     = Session.create(Map("user" -> "john"), 3600)
    val originalId  = session.id
    val regenerated = session.regenerateId

    assert(regenerated.id != originalId)
    assertEquals(regenerated.data, session.data)
    assertEquals(regenerated.expiresAt, session.expiresAt)
  }

  test("Session.extend updates expiration time") {
    val session  = Session.create(1800) // Start with shorter duration
    val original = session.expiresAt
    val extended = session.extend(3600) // Extend by longer duration

    assert(extended.expiresAt.isAfter(original))
    // Should be approximately 3600 seconds after now
    val expectedExpiry = Instant.now().plusSeconds(3600)
    assert(
      Math.abs(
        extended.expiresAt.getEpochSecond - expectedExpiry.getEpochSecond
      ) <= 1
    )
  }
}
