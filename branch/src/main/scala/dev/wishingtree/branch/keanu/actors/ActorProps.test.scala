package dev.wishingtree.branch.keanu.actors

class ActorPropsTest extends munit.FunSuite {

  case class SubObject(a: String, b: Int)
  case class SomeActor(a: String, b: Int, c: SubObject) extends Actor {
    override def onMsg: PartialFunction[Any, Any] = ???
  }

  test("ActorProps.props") {
    val props = ActorProps.props[SomeActor](("a", 1, SubObject("a", 1)))
    assertEquals(props.create(), SomeActor("a", 1, SubObject("a", 1)))
  }

}
