package dev.alteration.branch.spider.server.middleware

import java.time.Instant

class SessionStoreSpec extends munit.FunSuite {

  test("InMemorySessionStore saves and retrieves sessions") {
    val store   = InMemorySessionStore()
    val session = Session.create(3600)

    store.save(session)
    val retrieved = store.get(session.id)

    assertEquals(retrieved, Some(session))
  }

  test("InMemorySessionStore returns None for non-existent sessions") {
    val store = InMemorySessionStore()

    assertEquals(store.get("non-existent-id"), None)
  }

  test("InMemorySessionStore deletes sessions") {
    val store   = InMemorySessionStore()
    val session = Session.create(3600)

    store.save(session)
    assert(store.exists(session.id))

    store.delete(session.id)
    assert(!store.exists(session.id))
  }

  test("InMemorySessionStore does not return expired sessions") {
    val store   = InMemorySessionStore()
    val session = Session(
      id = "test-id",
      expiresAt = Instant.now().minusSeconds(1)
    )

    store.save(session)
    assertEquals(store.get("test-id"), None)
  }

  test("InMemorySessionStore cleanup removes expired sessions") {
    val store          = InMemorySessionStore()
    val validSession   = Session.create(3600)
    val expiredSession = Session(
      id = "expired-id",
      expiresAt = Instant.now().minusSeconds(1)
    )

    store.save(validSession)
    store.save(expiredSession)

    assertEquals(store.size, 2)

    store.cleanup()

    assertEquals(store.size, 1)
    assert(store.exists(validSession.id))
    assert(!store.exists(expiredSession.id))
  }

  test("InMemorySessionStore clear removes all sessions") {
    val store    = InMemorySessionStore()
    val session1 = Session.create(3600)
    val session2 = Session.create(3600)

    store.save(session1)
    store.save(session2)

    assertEquals(store.size, 2)

    store.clear()

    assertEquals(store.size, 0)
  }

  test("InMemorySessionStore overwrites existing sessions") {
    val store          = InMemorySessionStore()
    val session        = Session.create(Map("user" -> "john"), 3600)
    val updatedSession = session.set("role", "admin")

    store.save(session)
    store.save(updatedSession)

    val retrieved = store.get(session.id)
    assertEquals(retrieved.flatMap(_.get("role")), Some("admin"))
  }

  test("FileSessionStore saves and retrieves sessions") {
    val tmpDir = java.io.File.createTempFile("session-test", "")
    tmpDir.delete()
    tmpDir.mkdir()

    try {
      val store   = FileSessionStore(tmpDir)
      val session =
        Session.create(Map("user" -> "john", "role" -> "admin"), 3600)

      store.save(session)
      val retrieved = store.get(session.id)

      assert(retrieved.isDefined)
      assertEquals(retrieved.get.id, session.id)
      assertEquals(retrieved.get.data, session.data)
      assertEquals(
        retrieved.get.createdAt.toEpochMilli,
        session.createdAt.toEpochMilli
      )
    } finally {
      // Cleanup
      tmpDir.listFiles().foreach(_.delete())
      tmpDir.delete()
    }
  }

  test("FileSessionStore deletes sessions") {
    val tmpDir = java.io.File.createTempFile("session-test", "")
    tmpDir.delete()
    tmpDir.mkdir()

    try {
      val store   = FileSessionStore(tmpDir)
      val session = Session.create(3600)

      store.save(session)
      assert(store.exists(session.id))

      store.delete(session.id)
      assert(!store.exists(session.id))
    } finally {
      tmpDir.listFiles().foreach(_.delete())
      tmpDir.delete()
    }
  }

  test("FileSessionStore does not return expired sessions") {
    val tmpDir = java.io.File.createTempFile("session-test", "")
    tmpDir.delete()
    tmpDir.mkdir()

    try {
      val store   = FileSessionStore(tmpDir)
      val session = Session(
        id = "test-id",
        expiresAt = Instant.now().minusSeconds(1)
      )

      store.save(session)
      assertEquals(store.get("test-id"), None)
    } finally {
      tmpDir.listFiles().foreach(_.delete())
      tmpDir.delete()
    }
  }

  test("FileSessionStore cleanup removes expired sessions") {
    val tmpDir = java.io.File.createTempFile("session-test", "")
    tmpDir.delete()
    tmpDir.mkdir()

    try {
      val store          = FileSessionStore(tmpDir)
      val validSession   = Session.create(3600)
      val expiredSession = Session(
        id = "expired-id",
        expiresAt = Instant.now().minusSeconds(1)
      )

      store.save(validSession)
      store.save(expiredSession)

      assert(tmpDir.listFiles().length == 2)

      store.cleanup()

      assert(tmpDir.listFiles().length == 1)
      assert(store.exists(validSession.id))
      assert(!store.exists(expiredSession.id))
    } finally {
      tmpDir.listFiles().foreach(_.delete())
      tmpDir.delete()
    }
  }
}
