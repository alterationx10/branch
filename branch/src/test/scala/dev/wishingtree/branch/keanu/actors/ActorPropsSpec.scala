package dev.wishingtree.branch.keanu.actors

class ActorPropsSpec extends munit.FunSuite {

  // Test cases
  case class SubObject(a: String, b: Int)
  case class SimpleActor(name: String)                     extends Actor {
    override def onMsg: PartialFunction[Any, Any] = ???
  }
  case class ComplexActor(a: String, b: Int, c: SubObject) extends Actor {
    override def onMsg: PartialFunction[Any, Any] = ???
  }
  case class NoArgActor()                                  extends Actor {
    override def onMsg: PartialFunction[Any, Any] = ???
  }

  test("ActorProps.props - simple actor with single parameter") {
    val props = ActorProps.props[SimpleActor](Tuple1("test-actor"))
    assertEquals(props.create(), SimpleActor("test-actor"))
  }

  test("ActorProps.props - complex actor with nested case class") {
    val props = ActorProps.props[ComplexActor](("a", 1, SubObject("a", 1)))
    assertEquals(props.create(), ComplexActor("a", 1, SubObject("a", 1)))
  }

  test("ActorProps.props - actor with no arguments") {
    val props = ActorProps.props[NoArgActor](EmptyTuple)
    assertEquals(props.create(), NoArgActor())
  }

  test("ActorProps - identifier should contain class name") {
    val props = ActorProps.props[SimpleActor](Tuple1("test"))
    assert(props.identifier.contains("SimpleActor"))
  }

  test("ActorProps - multiple creates should create independent instances") {
    val props  = ActorProps.props[SimpleActor](Tuple1("test"))
    val actor1 = props.create()
    val actor2 = props.create()
    assert(actor1 ne actor2)     // Check they are different instances
    assertEquals(actor1, actor2) // But equal in value
  }
}
