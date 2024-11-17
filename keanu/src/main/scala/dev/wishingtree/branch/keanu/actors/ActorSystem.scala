package dev.wishingtree.branch.keanu.actors

import dev.wishingtree.branch.keanu.eventbus.{EventBus, EventMessage}

import java.util.concurrent.*
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.util.*

trait ActorSystem {

  val executorService: ExecutorService =
    Executors.newVirtualThreadPerTaskExecutor()

  private object LifecycleEventBus extends EventBus[LifecycleEvent]

  LifecycleEventBus.subscribe {
    case PoisonPill                    => ()
    case InterruptedTermination(refId) => ()
    case OnMsgTermination(refId, e)    => {
      val currentRef   = actorRefs(refId)
      val runningActor = runningActors(refId)
      runningActor.cancel(true)
      runningActors += refId -> submitActor(refId, currentRef.mailBox)
    }
  }

  val props: mutable.Map[String, ActorContext[?]]                    = mutable.Map.empty
  val actorRefs: mutable.Map[ActorRefId, ActorRef]                   = mutable.Map.empty
  val runningActors: mutable.Map[ActorRefId, CompletableFuture[Any]] =
    mutable.Map.empty

  def registerProp(prop: ActorContext[?]): Unit = synchronized {
    props += (prop.identifier -> prop)
  }

  private def registerActor(refId: ActorRefId, actor: ActorRef): ActorRef =
    synchronized {
      actorRefs.getOrElseUpdate(refId, actor)
    }

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

  def actorForName[A <: Actor: ClassTag](name: String): ActorRef =
    synchronized {
      val refId = ActorRefId[A](name)
      val ar    = actorRefs.getOrElseUpdate(
        refId,
        ActorRef(new LinkedBlockingQueue[Any]())
      )
      runningActors.getOrElseUpdate(
        refId,
        submitActor(refId, ar.mailBox)
      )
      ar
    }

  def shutdownAwait: Unit = synchronized {
    println(s"Finalizing ${actorRefs.size} actors")
    actorRefs.values.foreach { ref =>
      ref.tell(PoisonPill)
    }
    println(s"Shutting down ${runningActors.size} actors")
    runningActors.values.foreach { a =>
      Try(a.get())
    }

  }

}

object ActorSystem {
  def apply(): ActorSystem = new ActorSystem {}
}
