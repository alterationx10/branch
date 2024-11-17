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

  private def startOrRestartActor(refId: ActorRefId): Unit = {
    val currentRef = actorRefs(refId)
    runningActors.get(refId).foreach(_.cancel(true))
    runningActors += (refId -> submitActor(refId, currentRef.mailBox))
  }

  LifecycleEventBus.subscribe {
    case InterruptedTermination(refId) => startOrRestartActor(refId)
    case OnMsgTermination(refId, e)    => startOrRestartActor(refId)
    case _                             => ()
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

  private def actorForName[A <: Actor: ClassTag](name: String): ActorRef =
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

  def tell[A <: Actor : ClassTag](name: String, msg: Any): Unit =
    actorForName[A](name).tell(msg)
  
}

object ActorSystem {
  def apply(): ActorSystem = new ActorSystem {}
}
