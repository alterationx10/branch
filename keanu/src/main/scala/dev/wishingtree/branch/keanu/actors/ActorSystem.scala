package dev.wishingtree.branch.keanu.actors

import dev.wishingtree.branch.keanu.eventbus.{EventBus, EventMessage}

import java.util.concurrent.*
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.util.*

trait ActorSystem {

  private type Mailbox  = BlockingQueue[Any]
  private type ActorRef = CompletableFuture[Any]

  val executorService: ExecutorService =
    Executors.newVirtualThreadPerTaskExecutor()

  private object LifecycleEventBus extends EventBus[LifecycleEvent]

  private def startOrRestartActor(refId: ActorRefId): Unit = {
    val currentRef = mailboxes(refId)
    actors.get(refId).foreach(_.cancel(true))
    actors += (refId -> submitActor(refId, currentRef))
  }

  LifecycleEventBus.subscribe {
    case InterruptedTermination(refId)    => startOrRestartActor(refId)
    case OnMsgTermination(refId, e)       => startOrRestartActor(refId)
    case PoisonPillTermination(refId)     =>
      synchronized {
        mailboxes -= refId
        actors -= refId
      }
    case InitializationTermination(refId) =>
      synchronized {
        mailboxes -= refId
        actors -= refId
      }
    case _                                => ()
  }

  private val props: mutable.Map[String, ActorContext[?]] =
    mutable.Map.empty
  private val mailboxes: mutable.Map[ActorRefId, Mailbox] =
    mutable.Map.empty
  private val actors: mutable.Map[ActorRefId, ActorRef]   =
    mutable.Map.empty

  def registerProp(prop: ActorContext[?]): Unit = synchronized {
    props += (prop.identifier -> prop)
  }

  private def registerMailbox(
      refId: ActorRefId,
      mailbox: Mailbox
  ): Mailbox =
    synchronized {
      mailboxes.getOrElseUpdate(refId, mailbox)
    }

  private def submitActor(
      refId: ActorRefId,
      mailbox: Mailbox
  ): ActorRef = {
    CompletableFuture.supplyAsync[Any](
      () => {

        try {
          val newActor: Actor =
            props(refId.propId).create()
          while (true) {
            mailbox.take() match {
              case PoisonPill => throw PoisonPillException
              case msg: Any   => newActor.onMsg(msg)
            }
          }
        } catch {
          case PoisonPillException       =>
            LifecycleEventBus.publish(
              EventMessage("", PoisonPillTermination(refId))
            )
          case e: InterruptedException   =>
            Thread.currentThread().interrupt()
          case InstantiationException(e) =>
            LifecycleEventBus.publish(
              EventMessage("", InitializationTermination(refId))
            )
          case e                         =>
            LifecycleEventBus.publish(
              EventMessage("", OnMsgTermination(refId, e))
            )
        }
      },
      executorService
    )
  }

  private def actorForName[A <: Actor: ClassTag](
      name: String
  ): Mailbox =
    synchronized {
      val refId   = ActorRefId[A](name)
      val mailbox = mailboxes.getOrElseUpdate(
        refId,
        new LinkedBlockingQueue[Any]()
      )
      actors.getOrElseUpdate(
        refId,
        submitActor(refId, mailbox)
      )
      mailbox
    }

  def shutdownAwait: Unit = synchronized {
    println(s"Finalizing ${mailboxes.size} actors")
    mailboxes.values.foreach { mailbox =>
      mailbox.put(PoisonPill)
    }
    println(s"Shutting down ${actors.size} actors")
    actors.values.foreach { a =>
      Try(a.get())
    }

  }

  def tell[A <: Actor: ClassTag](name: String, msg: Any): Unit =
    actorForName[A](name).put(msg)

}

object ActorSystem {
  def apply(): ActorSystem = new ActorSystem {}
}
