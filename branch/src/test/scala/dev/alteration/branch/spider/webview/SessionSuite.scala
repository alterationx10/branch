package dev.alteration.branch.spider.webview

import munit.FunSuite

/** Tests for the Session class.
  *
  * Tests session data storage and manipulation.
  */
class SessionSuite extends FunSuite {

  test("empty session has no data") {
    val session = Session()
    assert(session.data.isEmpty)
  }

  test("session with initial data") {
    val session = Session(Map("userId" -> "123", "role" -> "admin"))
    assertEquals(session.data.size, 2)
    assertEquals(session.get[String]("userId"), Some("123"))
    assertEquals(session.get[String]("role"), Some("admin"))
  }

  test("get returns Some for existing key") {
    val session = Session(Map("name" -> "Alice"))
    assertEquals(session.get[String]("name"), Some("Alice"))
  }

  test("get returns None for missing key") {
    val session = Session()
    assertEquals(session.get[String]("missing"), None)
  }

  test("get with type casting") {
    val session = Session(Map("count" -> 42, "active" -> true))
    assertEquals(session.get[Int]("count"), Some(42))
    assertEquals(session.get[Boolean]("active"), Some(true))
  }

  test("put adds new key-value pair") {
    val session    = Session()
    val newSession = session.put("userId", "456")

    assertEquals(newSession.get[String]("userId"), Some("456"))
  }

  test("put updates existing key") {
    val session    = Session(Map("count" -> 1))
    val newSession = session.put("count", 2)

    assertEquals(newSession.get[Int]("count"), Some(2))
  }

  test("put is immutable - original session unchanged") {
    val session    = Session(Map("name" -> "Alice"))
    val newSession = session.put("name", "Bob")

    assertEquals(session.get[String]("name"), Some("Alice"))
    assertEquals(newSession.get[String]("name"), Some("Bob"))
  }

  test("remove deletes key") {
    val session    = Session(Map("token" -> "abc123", "userId" -> "1"))
    val newSession = session.remove("token")

    assertEquals(newSession.get[String]("token"), None)
    assertEquals(newSession.get[String]("userId"), Some("1"))
  }

  test("remove on missing key returns same data") {
    val session    = Session(Map("key" -> "value"))
    val newSession = session.remove("nonexistent")

    assertEquals(newSession.data, session.data)
  }

  test("remove is immutable - original session unchanged") {
    val session    = Session(Map("key" -> "value"))
    val newSession = session.remove("key")

    assertEquals(session.get[String]("key"), Some("value"))
    assertEquals(newSession.get[String]("key"), None)
  }

  test("chaining operations") {
    val session = Session()
      .put("userId", "123")
      .put("role", "admin")
      .put("temp", "data")
      .remove("temp")

    assertEquals(session.get[String]("userId"), Some("123"))
    assertEquals(session.get[String]("role"), Some("admin"))
    assertEquals(session.get[String]("temp"), None)
  }

  test("session stores different types") {
    val session = Session(
      Map(
        "string"  -> "text",
        "int"     -> 42,
        "double"  -> 3.14,
        "boolean" -> true,
        "list"    -> List(1, 2, 3),
        "map"     -> Map("a" -> 1)
      )
    )

    assertEquals(session.get[String]("string"), Some("text"))
    assertEquals(session.get[Int]("int"), Some(42))
    assertEquals(session.get[Double]("double"), Some(3.14))
    assertEquals(session.get[Boolean]("boolean"), Some(true))
    assertEquals(session.get[List[Int]]("list"), Some(List(1, 2, 3)))
    assertEquals(session.get[Map[String, Int]]("map"), Some(Map("a" -> 1)))
  }

  test("session can store custom case classes") {
    case class User(id: String, name: String, age: Int)
    val user    = User("123", "Alice", 30)
    val session = Session(Map("currentUser" -> user))

    assertEquals(session.get[User]("currentUser"), Some(user))
  }

  test("multiple put operations") {
    var session = Session()
    session = session.put("key1", "value1")
    session = session.put("key2", "value2")
    session = session.put("key3", "value3")

    assertEquals(session.data.size, 3)
    assertEquals(session.get[String]("key1"), Some("value1"))
    assertEquals(session.get[String]("key2"), Some("value2"))
    assertEquals(session.get[String]("key3"), Some("value3"))
  }

  test("overwriting values") {
    val session = Session(Map("counter" -> 0))
      .put("counter", 1)
      .put("counter", 2)
      .put("counter", 3)

    assertEquals(session.get[Int]("counter"), Some(3))
  }
}
