package dev.alteration.branch.piggy

import munit.FunSuite

class NamedParameterParserSpec extends FunSuite {

  test("Parse simple named parameter") {
    val (sql, params) =
      NamedParameterParser.parse("SELECT * FROM users WHERE name = :name")
    assertEquals(sql, "SELECT * FROM users WHERE name = ?")
    assertEquals(params, Seq("name"))
  }

  test("Parse multiple named parameters") {
    val (sql, params) = NamedParameterParser.parse(
      "INSERT INTO users (name, age) VALUES (:name, :age)"
    )
    assertEquals(sql, "INSERT INTO users (name, age) VALUES (?, ?)")
    assertEquals(params, Seq("name", "age"))
  }

  test("Ignore parameters in single-quoted string literals") {
    val (sql, params) = NamedParameterParser.parse(
      "SELECT * FROM person WHERE name = :name AND name ILIKE '%:test%'"
    )
    assertEquals(
      sql,
      "SELECT * FROM person WHERE name = ? AND name ILIKE '%:test%'"
    )
    assertEquals(params, Seq("name"))
  }

  test("Ignore parameters in double-quoted identifiers") {
    val (sql, params) =
      NamedParameterParser.parse("SELECT * FROM \":table\" WHERE name = :name")
    assertEquals(sql, "SELECT * FROM \":table\" WHERE name = ?")
    assertEquals(params, Seq("name"))
  }

  test("Handle escaped single quotes") {
    val (sql, params) = NamedParameterParser.parse(
      "SELECT * FROM person WHERE name = :name AND note = 'It''s :test'"
    )
    assertEquals(
      sql,
      "SELECT * FROM person WHERE name = ? AND note = 'It''s :test'"
    )
    assertEquals(params, Seq("name"))
  }

  test("Handle escaped double quotes") {
    val (sql, params) = NamedParameterParser.parse(
      "SELECT * FROM \"table\"\"name\" WHERE id = :id"
    )
    assertEquals(sql, "SELECT * FROM \"table\"\"name\" WHERE id = ?")
    assertEquals(params, Seq("id"))
  }

  test("Handle standalone colons") {
    val (sql, params) = NamedParameterParser.parse(
      "SELECT * FROM users WHERE time > '12:30:00' AND name = :name"
    )
    assertEquals(
      sql,
      "SELECT * FROM users WHERE time > '12:30:00' AND name = ?"
    )
    assertEquals(params, Seq("name"))
  }
}
