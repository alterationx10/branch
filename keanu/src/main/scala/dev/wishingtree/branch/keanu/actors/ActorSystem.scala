package dev.wishingtree.branch.keanu.actors

import dev.wishingtree.branch.keanu

import java.util.concurrent.*
import scala.collection.*
import scala.reflect.ClassTag
import scala.util.*

trait ActorSystem {

  private type Mailbox  = BlockingQueue[Any]
  private type ActorRef = CompletableFuture[LifecycleEvent]

  private val props: concurrent.Map[String, ActorContext[?]] =
    concurrent.TrieMap.empty
  private val mailboxes: concurrent.Map[ActorRefId, Mailbox] =
    concurrent.TrieMap.empty
  private val actors: concurrent.Map[ActorRefId, ActorRef]   =
    concurrent.TrieMap.empty

  val executorService: ExecutorService =
    Executors.newVirtualThreadPerTaskExecutor()

  private def restartActor(refId: ActorRefId): Unit = {
    val mailbox = getOrCreateMailbox(refId)
    actors -= refId
    actors += (refId -> submitActor(refId, mailbox))
  }

  private def unregisterMailboxAndActor(refId: ActorRefId) = {
    mailboxes -= refId
    actors -= refId
  }

  def registerProp(prop: ActorContext[?]): Unit =
    props += (prop.identifier -> prop)

  private def getOrCreateMailbox(
      refId: ActorRefId
  ): Mailbox =
    mailboxes.getOrElseUpdate(refId, new LinkedBlockingQueue[Any]())

  private def submitActor(
      refId: ActorRefId,
      mailbox: Mailbox
  ): ActorRef = {
    CompletableFuture.supplyAsync[LifecycleEvent](
      () => {

        val terminationResult: LifecycleEvent = {
          try {
            val newActor: Actor =
              props(refId.propId).create()
            while (true) {
              mailbox.take() match {
                case PoisonPill => throw PoisonPillException
                case msg: Any   => newActor.onMsg(msg)
              }
            }
            UnexpectedTermination
          } catch {
            case PoisonPillException       =>
              unregisterMailboxAndActor(refId)
              PoisonPillTermination
            case e: InterruptedException   =>
              unregisterMailboxAndActor(refId)
              InterruptedTermination
            case InstantiationException(e) =>
              unregisterMailboxAndActor(refId)
              InitializationTermination
            case e: CancellationException  =>
              unregisterMailboxAndActor(refId)
              CancellationTermination
            case e                         =>
              restartActor(refId)
              OnMsgTermination(e)
          }
        }
        terminationResult
      },
      executorService
    )
  }

  private def actorForName[A <: Actor: ClassTag](
      name: String
  ): Mailbox = {
    val refId =
      ActorRefId[A](name)

    val mailbox = getOrCreateMailbox(refId)

    if !actors.contains(refId) then
      actors.addOne(refId -> submitActor(refId, mailbox))

    mailbox
  }

  private def cleanUp: Boolean = {
    // PoisonPill should cause the actor to clean itself up
    mailboxes.values.foreach { mailbox =>
      mailbox.put(PoisonPill)
    }
    actors.map((id, a) => {
      Try(a.join()) // Wait for known actors to terminate
    })
    // Presumably, getting the values means they have unregistered themselves,
    // and any more actors were created afterward, and need to be cleaned up
    actors.nonEmpty
  }

  def shutdownAwait: Unit = {
    var n = 0
    while (cleanUp) {
      n += 1
    }
    if n == 1 then println(s"One and done!")
    else println(s"Bad code ran $n times and you should feel bad :sad:")
  }

  def tell[A <: Actor: ClassTag](name: String, msg: Any): Unit = {
    actorForName[A](name).put(msg)
  }

}

object ActorSystem {
  def apply(): ActorSystem = new ActorSystem {}
}
