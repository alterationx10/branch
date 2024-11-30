package dev.wishingtree.branch.keanu.actors

import dev.wishingtree.branch.keanu
import dev.wishingtree.branch.macaroni.runtimes.BranchExecutors

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.*
import scala.reflect.ClassTag
import scala.util.*

/** The default ActorSystem implementation. For singleton usage, have an object
  * extend this trait. For an instance usage, the default apply method can be
  * used.
  */
trait ActorSystem {

  /** A flag for internally controlling if the ActorSystem is shutting down */
  private final val isShuttingDown: AtomicBoolean =
    new AtomicBoolean(false)

  /** Returns true if the ActorSystem is shutting down */
  final def isShutdown: Boolean =
    isShuttingDown.get()

  private final type Mailbox  = BlockingQueue[Any]
  private final type ActorRef = CompletableFuture[LifecycleEvent]

  /** A collection of props which now how to create actors
    */
  private final val props: concurrent.Map[String, ActorProps[?]] =
    concurrent.TrieMap.empty

  /** A collection of mailboxes used to deliver messages to actors
    */
  private final val mailboxes: concurrent.Map[ActorRefId, Mailbox] =
    concurrent.TrieMap.empty

  /** A collection of currently running actors
    */
  private final val actors: concurrent.Map[ActorRefId, ActorRef] =
    concurrent.TrieMap.empty

  /** The executor service used to run actors
    */
  val executorService: ExecutorService =
    BranchExecutors.executorService

  /** Ensure there is a mailbox and running actor for the given refId
    * @param refId
    */
  private final def restartActor(refId: ActorRefId): Unit = {
    val mailbox = getOrCreateMailbox(refId)
    actors -= refId
    actors += (refId -> submitActor(refId, mailbox))
  }

  /** Remove the mailbox and actor for the given refId from the system
    * @param refId
    * @return
    */
  private final def unregisterMailboxAndActor(refId: ActorRefId) = {
    mailboxes -= refId
    actors -= refId
  }

  /** Register a prop with the system, so it can be used to create actors
    * @param prop
    */
  final def registerProp(prop: ActorProps[?]): Unit =
    props += (prop.identifier -> prop)

  /** Get or create a mailbox for the given refId
    * @param refId
    * @return
    */
  private final def getOrCreateMailbox(
      refId: ActorRefId
  ): Mailbox =
    mailboxes.getOrElseUpdate(refId, new LinkedBlockingQueue[Any]())

  /** Submit an actor to the executor service
    * @param refId
    * @param mailbox
    * @return
    */
  private final def submitActor(
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

  /** Get or create an actor for the given name.
    * @param name
    * @tparam A
    * @return
    */
  private final def actorForName[A <: Actor: ClassTag](
      name: String
  ): Mailbox = {
    val refId =
      ActorRefId[A](name)

    val mailbox = getOrCreateMailbox(refId)

    if !actors.contains(refId) then
      actors.addOne(refId -> submitActor(refId, mailbox))

    mailbox
  }

  /** Try to clean up the system by sending PoisonPill to all actors and waiting
    * for them to terminate.
    * @return
    */
  private final def cleanUp: Boolean = {
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

  /** Shutdown the actor system and wait for all actors to terminate. All calls
    * to tell will throw an exception after this is called.
    */
  final def shutdownAwait(): Unit = {
    isShuttingDown.set(true)
    cleanUp
  }

  /** Tell an actor to process a message. If the actor does not exist, it will
    * be created.
    */
  final def tell[A <: Actor: ClassTag](name: String, msg: Any): Unit = {
    if !isShuttingDown.get() then actorForName[A](name).put(msg)
    else throw new IllegalStateException("ActorSystem is shutting down")
  }

}

object ActorSystem {
  def apply(): ActorSystem = new ActorSystem {}
}
