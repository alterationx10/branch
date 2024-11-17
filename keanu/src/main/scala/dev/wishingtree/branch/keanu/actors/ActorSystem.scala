package dev.wishingtree.branch.keanu.actors

import java.util.concurrent.{
  BlockingQueue,
  CompletableFuture,
  ExecutorService,
  Executors,
  LinkedBlockingQueue
}
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

trait ActorSystem {
  
  
  val executorService: ExecutorService =
    Executors.newVirtualThreadPerTaskExecutor()

  private def nameOf[A <: Actor: ClassTag]: String =
    classTag[A].getClass.getCanonicalName

  val actors: mutable.Map[(String, String), ActorRef] = mutable.Map.empty
  val props: mutable.Map[String, ActorContext[?]]     = mutable.Map.empty

  def registerProp(prop: ActorContext[?]): Unit = {
    props += (prop.identifier -> prop)
  }

  private def registerActor(id: (String, String), actor: ActorRef): ActorRef = {
    actors.getOrElseUpdate(id, actor)
  }

  def unregisterActor[A <: Actor: ClassTag](name: String): Unit =
    actors -= (name -> nameOf[A])

  private def submitActor(
      propId: String,
      mailbox: BlockingQueue[Any]
  ): CompletableFuture[Any] = {
    CompletableFuture.supplyAsync[Any](
      () => {
        val newActor: Actor =
          props(propId).create()
        try {
          while (true) {
            mailbox.take() match {
              case PoisonPill => throw PoisonPillException
              case msg: Any   => newActor.onMsg(msg)
            }
          }
        } catch {
          case PoisonPillException => ()
          case e: InterruptedException => Thread.currentThread().interrupt()
        }
      },
      executorService
    )
  }

  def actorForName[A <: Actor: ClassTag](name: String): ActorRef = {
    val key = name -> nameOf[A]
    actors.getOrElseUpdate(
      key, {
        val mailbox: BlockingQueue[Any] =
          new LinkedBlockingQueue[Any]()

        val submission =
          submitActor(nameOf[A], mailbox)
        ActorRef(mailbox, submission)
      }
    )
  }

  def shutdownAwait: Unit = {
    actors.values.foreach { ref =>
      ref.tell(PoisonPill)
      ref.actorFuture.get()
    }
  }

}

object ActorSystem {
  def apply(): ActorSystem = new ActorSystem {}
}
