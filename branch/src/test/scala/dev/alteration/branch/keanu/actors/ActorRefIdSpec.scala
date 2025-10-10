package dev.alteration.branch.keanu.actors

import munit.FunSuite

class ActorRefIdSpec extends FunSuite {

  case class TestActor() extends Actor {
    override def onMsg: PartialFunction[Any, Any] = PartialFunction.empty
  }

  test("ActorRefId.apply should create identifier from name and type") {
    val refId = ActorRefId[TestActor]("myActor")
    assertEquals(refId.name, "myActor")
    assert(
      refId.propId.contains("TestActor"),
      "propId should contain class name"
    )
  }

  test("ActorRefId.toIdentifier should create parseable string") {
    val refId      = ActorRefId[TestActor]("myActor")
    val identifier = refId.toIdentifier

    assert(identifier.contains("myActor"), "Identifier should contain name")
    assert(identifier.contains(":"), "Identifier should contain separator")
  }

  test("ActorRefId.fromIdentifier should parse valid identifier") {
    val original   = ActorRefId[TestActor]("myActor")
    val identifier = original.toIdentifier
    val parsed     = ActorRefId.fromIdentifier(identifier)

    assertEquals(parsed, Some(original), "Should parse back to original")
  }

  test("ActorRefId.fromIdentifier should handle roundtrip correctly") {
    val refId1 = ActorRefId[TestActor]("actor1")
    val refId2 = ActorRefId[TestActor]("actor2")

    val parsed1 = ActorRefId.fromIdentifier(refId1.toIdentifier)
    val parsed2 = ActorRefId.fromIdentifier(refId2.toIdentifier)

    assertEquals(parsed1, Some(refId1))
    assertEquals(parsed2, Some(refId2))
  }

  test(
    "ActorRefId.fromIdentifier should return None for invalid format - no colon"
  ) {
    val result = ActorRefId.fromIdentifier("invalididentifier")
    assertEquals(
      result,
      None,
      "Should return None for identifier without colon"
    )
  }

  test(
    "ActorRefId.fromIdentifier should return None for invalid format - too many colons"
  ) {
    val result = ActorRefId.fromIdentifier("name:class:extra")
    assertEquals(
      result,
      None,
      "Should return None for identifier with too many parts"
    )
  }

  test(
    "ActorRefId.fromIdentifier should return None for invalid format - empty string"
  ) {
    val result = ActorRefId.fromIdentifier("")
    assertEquals(result, None, "Should return None for empty string")
  }

  test(
    "ActorRefId.fromIdentifier should return None for invalid format - only colon"
  ) {
    val result = ActorRefId.fromIdentifier(":")
    assertEquals(
      result,
      None,
      "Should return None for identifier with only colon"
    )
  }

  test(
    "ActorRefId.fromIdentifier should handle names with special characters"
  ) {
    val refId  = ActorRefId("actor-name_123", "com.example.MyActor")
    val parsed = ActorRefId.fromIdentifier(refId.toIdentifier)
    assertEquals(
      parsed,
      Some(refId),
      "Should handle special characters in name"
    )
  }
}
