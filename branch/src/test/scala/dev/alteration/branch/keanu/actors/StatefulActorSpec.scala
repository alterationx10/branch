package dev.alteration.branch.keanu.actors

import munit.FunSuite

import java.util.concurrent.CountDownLatch
import scala.concurrent.duration._

class StatefulActorSpec extends FunSuite {

  test("StatefulActor should maintain state across messages") {
    case class CounterState(count: Int)

    sealed trait CounterMessage
    case object Increment                      extends CounterMessage
    case object Decrement                      extends CounterMessage
    case class GetCount(latch: CountDownLatch) extends CounterMessage

    @volatile var finalCount = 0

    case class CounterActor()
        extends StatefulActor[CounterState, CounterMessage] {
      override def initialState: CounterState = CounterState(0)

      override def receive: PartialFunction[CounterMessage, CounterState] = {
        case Increment   => state.copy(count = state.count + 1)
        case Decrement   => state.copy(count = state.count - 1)
        case GetCount(l) =>
          finalCount = state.count
          l.countDown()
          state
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[CounterActor](EmptyTuple)
    as.registerProp(props)

    val latch = new CountDownLatch(1)
    as.tell[CounterActor]("test", Increment)
    as.tell[CounterActor]("test", Increment)
    as.tell[CounterActor]("test", Increment)
    as.tell[CounterActor]("test", Decrement)
    as.tell[CounterActor]("test", GetCount(latch))

    latch.await(2, SECONDS)
    as.shutdownAwait()

    assertEquals(finalCount, 2, "State should be maintained across messages")
  }

  test("StatefulActor should use initial state") {
    case class MyState(value: String, counter: Int)

    sealed trait Message
    case object GetInitial extends Message

    @volatile var capturedState: Option[MyState] = None
    val latch                                    = new CountDownLatch(1)

    case class TestActor() extends StatefulActor[MyState, Message] {
      override def initialState: MyState = MyState("initial", 42)

      override def receive: PartialFunction[Message, MyState] = {
        case GetInitial =>
          capturedState = Some(state)
          latch.countDown()
          state
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[TestActor](EmptyTuple)
    as.registerProp(props)

    as.tell[TestActor]("test", GetInitial)
    latch.await(2, SECONDS)
    as.shutdownAwait()

    assert(capturedState.isDefined, "Should have captured state")
    assertEquals(capturedState.get.value, "initial")
    assertEquals(capturedState.get.counter, 42)
  }

  test("StatefulActor should handle complex state transitions") {
    case class TodoState(
        items: List[String],
        completed: Set[String],
        count: Int
    )

    sealed trait TodoMessage
    case class AddItem(item: String)           extends TodoMessage
    case class CompleteItem(item: String)      extends TodoMessage
    case class RemoveItem(item: String)        extends TodoMessage
    case class GetState(latch: CountDownLatch) extends TodoMessage

    @volatile var finalState: Option[TodoState] = None

    case class TodoActor() extends StatefulActor[TodoState, TodoMessage] {
      override def initialState: TodoState = TodoState(List.empty, Set.empty, 0)

      override def receive: PartialFunction[TodoMessage, TodoState] = {
        case AddItem(item)      =>
          state.copy(
            items = state.items :+ item,
            count = state.count + 1
          )
        case CompleteItem(item) =>
          state.copy(completed = state.completed + item)
        case RemoveItem(item)   =>
          state.copy(
            items = state.items.filterNot(_ == item),
            completed = state.completed - item
          )
        case GetState(l)        =>
          finalState = Some(state)
          l.countDown()
          state
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[TodoActor](EmptyTuple)
    as.registerProp(props)

    val latch = new CountDownLatch(1)
    as.tell[TodoActor]("test", AddItem("task1"))
    as.tell[TodoActor]("test", AddItem("task2"))
    as.tell[TodoActor]("test", AddItem("task3"))
    as.tell[TodoActor]("test", CompleteItem("task1"))
    as.tell[TodoActor]("test", RemoveItem("task2"))
    as.tell[TodoActor]("test", GetState(latch))

    latch.await(2, SECONDS)
    as.shutdownAwait()

    assert(finalState.isDefined, "Should have captured final state")
    assertEquals(finalState.get.items, List("task1", "task3"))
    assertEquals(finalState.get.completed, Set("task1"))
    assertEquals(finalState.get.count, 3)
  }

  test("StatefulActor should reset state on restart") {
    case class CounterState(count: Int)

    sealed trait Message
    case object Increment                      extends Message
    case object Fail                           extends Message
    case class GetCount(latch: CountDownLatch) extends Message

    @volatile var countBeforeFail = 0
    @volatile var countAfterFail  = 0
    val latch1                    = new CountDownLatch(1)
    val latch2                    = new CountDownLatch(1)

    case class RestartActor() extends StatefulActor[CounterState, Message] {
      override def initialState: CounterState = CounterState(0)

      override def receive: PartialFunction[Message, CounterState] = {
        case Increment   => state.copy(count = state.count + 1)
        case Fail        => throw new RuntimeException("Intentional failure")
        case GetCount(l) =>
          if (l == latch1) countBeforeFail = state.count
          else countAfterFail = state.count
          l.countDown()
          state
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[RestartActor](EmptyTuple)
    as.registerProp(props)

    as.tell[RestartActor]("test", Increment)
    as.tell[RestartActor]("test", Increment)
    as.tell[RestartActor]("test", GetCount(latch1))
    latch1.await(2, SECONDS)

    as.tell[RestartActor]("test", Fail)
    Thread.sleep(150) // Wait for restart

    as.tell[RestartActor]("test", GetCount(latch2))
    latch2.await(2, SECONDS)
    as.shutdownAwait()

    assertEquals(countBeforeFail, 2, "Count before fail should be 2")
    assertEquals(countAfterFail, 0, "Count after restart should reset to 0")
  }

  test("StatefulActor should preserve state across restarts if saved") {
    case class CounterState(count: Int)

    sealed trait Message
    case object Increment                      extends Message
    case object Fail                           extends Message
    case class GetCount(latch: CountDownLatch) extends Message

    @volatile var countBeforeFail = 0
    @volatile var countAfterFail  = 0
    val latch1                    = new CountDownLatch(1)
    val latch2                    = new CountDownLatch(1)

    // Use external storage to persist state across actor instances
    object StateStorage {
      @volatile var savedState: Option[CounterState] = None
    }

    case class PersistentActor() extends StatefulActor[CounterState, Message] {
      override def preRestart(reason: Throwable): Unit = {
        StateStorage.savedState = Some(state)
      }

      override def initialState: CounterState =
        StateStorage.savedState.getOrElse(CounterState(0))

      override def receive: PartialFunction[Message, CounterState] = {
        case Increment   => state.copy(count = state.count + 1)
        case Fail        => throw new RuntimeException("Intentional failure")
        case GetCount(l) =>
          if (l == latch1) countBeforeFail = state.count
          else countAfterFail = state.count
          l.countDown()
          state
      }
    }

    StateStorage.savedState = None // Reset for test
    val as    = ActorSystem()
    val props = ActorProps.props[PersistentActor](EmptyTuple)
    as.registerProp(props)

    as.tell[PersistentActor]("test", Increment)
    as.tell[PersistentActor]("test", Increment)
    as.tell[PersistentActor]("test", GetCount(latch1))
    latch1.await(2, SECONDS)

    as.tell[PersistentActor]("test", Fail)
    Thread.sleep(150) // Wait for restart

    as.tell[PersistentActor]("test", GetCount(latch2))
    latch2.await(2, SECONDS)
    as.shutdownAwait()

    assertEquals(countBeforeFail, 2, "Count before fail should be 2")
    assertEquals(countAfterFail, 2, "Count after restart should be preserved")
  }

  test("StatefulActor should work with ActorContext") {
    case class MyState(childCount: Int)

    sealed trait Message
    case object CreateChild                         extends Message
    case class GetChildCount(latch: CountDownLatch) extends Message

    @volatile var finalChildCount = 0

    case class ParentActor() extends StatefulActor[MyState, Message] {
      override def initialState: MyState = MyState(0)

      override def receive: PartialFunction[Message, MyState] = {
        case CreateChild      =>
          context.actorOf[ChildActor](s"child${state.childCount}")
          state.copy(childCount = state.childCount + 1)
        case GetChildCount(l) =>
          finalChildCount = context.children.size
          l.countDown()
          state
      }
    }

    case class ChildActor() extends TypedActor[Any] {
      override def typedOnMsg: PartialFunction[Any, Any] = { case _ => () }
    }

    val as     = ActorSystem()
    val props1 = ActorProps.props[ParentActor](EmptyTuple)
    val props2 = ActorProps.props[ChildActor](EmptyTuple)
    as.registerProp(props1)
    as.registerProp(props2)

    val latch = new CountDownLatch(1)
    as.tell[ParentActor]("parent", CreateChild)
    as.tell[ParentActor]("parent", CreateChild)
    as.tell[ParentActor]("parent", CreateChild)
    Thread.sleep(100) // Let children register
    as.tell[ParentActor]("parent", GetChildCount(latch))

    latch.await(2, SECONDS)
    as.shutdownAwait()

    assertEquals(finalChildCount, 3, "Should have created 3 children")
  }

  test("StatefulActor should work with lifecycle hooks") {
    case class MyState(value: Int)

    sealed trait Message
    case object Increment extends Message

    @volatile var preStartCalled   = false
    @volatile var postStopCalled   = false
    @volatile var preRestartCalled = false

    case class LifecycleActor() extends StatefulActor[MyState, Message] {
      override def initialState: MyState = MyState(0)

      override def preStart(): Unit                           = preStartCalled = true
      override def postStop(): Unit                           = postStopCalled = true
      override def preRestart(reason: Throwable): Unit        = preRestartCalled = true
      override def receive: PartialFunction[Message, MyState] = {
        case Increment => state.copy(value = state.value + 1)
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[LifecycleActor](EmptyTuple)
    as.registerProp(props)

    as.tell[LifecycleActor]("test", Increment)
    Thread.sleep(50)
    as.shutdownAwait()

    assert(preStartCalled, "preStart should be called")
    assert(postStopCalled, "postStop should be called")
    assert(!preRestartCalled, "preRestart should not be called")
  }

  test("StatefulActor should handle partial message matching") {
    case class MyState(handled: List[String])

    sealed trait Message
    case class Handled(value: String)   extends Message
    case class Unhandled(value: String) extends Message

    @volatile var finalState: Option[MyState] = None
    val latch                                 = new CountDownLatch(1)

    case class SelectiveActor() extends StatefulActor[MyState, Message] {
      override def initialState: MyState = MyState(List.empty)

      override def receive: PartialFunction[Message, MyState] = {
        case Handled(v) =>
          val newState = state.copy(handled = state.handled :+ v)
          if (v == "done") {
            finalState = Some(newState)
            latch.countDown()
          }
          newState
        // Unhandled intentionally not matched
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[SelectiveActor](EmptyTuple)
    as.registerProp(props)

    as.tell[SelectiveActor]("test", Handled("first"))
    as.tell[SelectiveActor]("test", Unhandled("ignored"))
    as.tell[SelectiveActor]("test", Handled("second"))
    as.tell[SelectiveActor]("test", Handled("done"))

    latch.await(2, SECONDS)
    as.shutdownAwait()

    assert(finalState.isDefined, "Should have captured final state")
    assertEquals(
      finalState.get.handled,
      List("first", "second", "done"),
      "Should only process handled messages"
    )

    val deadLetters = as.getDeadLetters(10)
    assert(
      deadLetters.exists(dl =>
        dl.message.asInstanceOf[Unhandled].value == "ignored"
      ),
      "Unhandled message should be in dead letters"
    )
  }

  test("StatefulActor should work with Ask pattern") {
    import scala.concurrent.Await

    case class MyState(counter: Int)

    sealed trait Message
    case object GetCounter extends Message

    case class QueryActor() extends StatefulActor[MyState, Any] {
      override def initialState: MyState = MyState(42)

      override def receive: PartialFunction[Any, MyState] = {
        case ask: Ask[?] =>
          ask.message match {
            case GetCounter => ask.complete(state.counter)
            case _          => ()
          }
          state
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[QueryActor](EmptyTuple)
    as.registerProp(props)

    val future = as.ask[QueryActor, Int]("test", GetCounter, 2.seconds)
    val result = Await.result(future, 3.seconds)

    as.shutdownAwait()

    assertEquals(result, 42)
  }

  test("StatefulActor should handle state-dependent behavior") {
    case class SessionState(authenticated: Boolean, username: Option[String])

    sealed trait Message
    case class Login(username: String) extends Message
    case object Logout                 extends Message
    case object GetUserInfo            extends Message

    @volatile var userInfo: Option[String] = None
    val latch                              = new CountDownLatch(1)

    case class SessionActor() extends StatefulActor[SessionState, Message] {
      override def initialState: SessionState =
        SessionState(authenticated = false, None)

      override def receive: PartialFunction[Message, SessionState] = {
        case Login(username) =>
          state.copy(authenticated = true, username = Some(username))
        case Logout          =>
          state.copy(authenticated = false, username = None)
        case GetUserInfo     =>
          userInfo = if (state.authenticated) state.username else None
          latch.countDown()
          state
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[SessionActor](EmptyTuple)
    as.registerProp(props)

    as.tell[SessionActor]("test", GetUserInfo) // Not authenticated yet
    Thread.sleep(50)
    as.tell[SessionActor]("test", Login("alice"))
    as.tell[SessionActor]("test", GetUserInfo)

    latch.await(2, SECONDS)
    as.shutdownAwait()

    assert(userInfo.isDefined, "User should be authenticated")
    assertEquals(userInfo.get, "alice")
  }

  test("StatefulActor should return state from receive") {
    case class MyState(value: Int)

    sealed trait Message
    case object Increment                      extends Message
    case class GetState(latch: CountDownLatch) extends Message

    @volatile var returnedValue: Option[Int] = None

    case class TestActor() extends StatefulActor[MyState, Message] {
      override def initialState: MyState = MyState(0)

      override def receive: PartialFunction[Message, MyState] = {
        case Increment   => state.copy(value = state.value + 1)
        case GetState(l) =>
          returnedValue = Some(state.value)
          l.countDown()
          state
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[TestActor](EmptyTuple)
    as.registerProp(props)

    val latch = new CountDownLatch(1)
    as.tell[TestActor]("test", Increment)
    as.tell[TestActor]("test", Increment)
    as.tell[TestActor]("test", GetState(latch))

    latch.await(2, SECONDS)
    as.shutdownAwait()

    assert(returnedValue.isDefined, "Should have returned value")
    assertEquals(returnedValue.get, 2)
  }
}
