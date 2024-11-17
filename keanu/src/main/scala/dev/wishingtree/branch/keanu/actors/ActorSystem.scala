package dev.wishingtree.branch.keanu.actors

import dev.wishingtree.branch.keanu.eventbus.{EventBus, EventMessage}

import java.util.concurrent.{
  BlockingQueue,
  CompletableFuture,
  ExecutorService,
  Executors,
  LinkedBlockingQueue
}
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}
import scala.util.*

trait ActorSystem {

  val executorService: ExecutorService =
    Executors.newVirtualThreadPerTaskExecutor()

  private object LifecycleEventBus extends EventBus[LifecycleEvent]

  LifecycleEventBus.subscribe {
    case PoisonPill                    => ()
    case InterruptedTermination(refId) => ()
    case OnMsgTermination(refId, e)    => {
      val currentRef = actors(refId)
      currentRef.actorFuture.cancel(true)
      val newFuture  = submitActor(refId, currentRef.mailBox)
      actors += (refId -> currentRef.restart(newFuture))
    }
  }

  private def nameOf[A <: Actor: ClassTag]: String =
    classTag[A].getClass.getCanonicalName

  val actors: mutable.Map[ActorRefId, ActorRef]   = mutable.Map.empty
  val props: mutable.Map[String, ActorContext[?]] = mutable.Map.empty

  def registerProp(prop: ActorContext[?]): Unit = {
    props += (prop.identifier -> prop)
  }

  private def registerActor(refId: ActorRefId, actor: ActorRef): ActorRef = {
    actors.getOrElseUpdate(refId, actor)
  }

  def unregisterActor[A <: Actor: ClassTag](name: String): Unit =
    actors -= ActorRefId[A](name)

  private def submitActor(
      refId: ActorRefId,
      mailbox: BlockingQueue[Any]
  ): CompletableFuture[Any] = {
    CompletableFuture.supplyAsync[Any](
      () => {
        val newActor: Actor =
          props(refId.propId).create()
        try {
          while (true) {
            mailbox.take() match {
              case PoisonPill => throw PoisonPillException
              case msg: Any   => newActor.onMsg(msg)
            }
          }
        } catch {
          case PoisonPillException => ()
          case e: InterruptedException => {
            Thread.currentThread().interrupt()
          }
          case e                       => {
            LifecycleEventBus.publish(
              EventMessage("", OnMsgTermination(refId, e))
            )
          }
        }
      },
      executorService
    )
  }

  def actorForName[A <: Actor: ClassTag](name: String): ActorRef = {
    val refId = ActorRefId[A](name)
    actors.getOrElseUpdate(
      refId, {
        val mailbox: BlockingQueue[Any] =
          new LinkedBlockingQueue[Any]()

        val submission =
          submitActor(refId, mailbox)
        ActorRef(mailbox, submission)
      }
    )
  }

  def shutdownAwait: Unit = {
    actors.values.foreach { ref =>
      println(s"Shutting dow ${actors.size} actors")
      ref.tell(PoisonPill)
      Try(ref.actorFuture.get())
    }
  }

}

object ActorSystem {
  def apply(): ActorSystem = new ActorSystem {}
}
