package dev.alteration.branch.keanu.actors

import dev.alteration.branch.macaroni.runtimes.BranchExecutors
import dev.alteration.branch.keanu

import java.time.Instant
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.*
import scala.jdk.CollectionConverters.*
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

  /** A queue for messages that could not be delivered or processed. Bounded to
    * prevent memory issues.
    */
  private final val deadLetters: BlockingQueue[DeadLetter] =
    new LinkedBlockingQueue[DeadLetter](10000)

  /** Returns recent dead letters from the queue (up to the specified limit).
    * Does not remove them from the queue.
    *
    * @param limit
    *   Maximum number of dead letters to return
    * @return
    *   List of dead letters
    */
  final def getDeadLetters(limit: Int = 100): List[DeadLetter] = {
    require(limit > 0, "Limit must be positive")
    deadLetters.asScala.take(limit).toList
  }

  /** Adds a dead letter to the queue. If the queue is full, the oldest entry
    * is silently dropped.
    *
    * @param deadLetter
    *   The dead letter to record
    */
  private final def recordDeadLetter(deadLetter: DeadLetter): Unit = {
    deadLetters.offer(deadLetter) // Non-blocking, returns false if full
    ()
  }

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
  final def registerProp(prop: ActorProps[?]): Unit = {
    require(prop != null, "ActorProps cannot be null")
    require(prop.identifier != null && prop.identifier.nonEmpty,
      "ActorProps identifier cannot be null or empty")
    props += (prop.identifier -> prop)
  }

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
            val newActor: Actor = try {
              props(refId.propId).create()
            } catch {
              case e: Exception => throw InstantiationException(e)
            }
            while (true) {
              mailbox.take() match {
                case PoisonPill => throw PoisonPillException
                case msg: Any   =>
                  if (newActor.onMsg.isDefinedAt(msg)) {
                    newActor.onMsg(msg)
                  } else {
                    // Message not handled by actor, record as dead letter
                    recordDeadLetter(
                      DeadLetter(
                        message = msg,
                        recipient = refId,
                        timestamp = Instant.now(),
                        reason = UnhandledMessage
                      )
                    )
                  }
              }
            }
            UnexpectedTermination
          } catch {
            case PoisonPillException       =>
              unregisterMailboxAndActor(refId)
              PoisonPillTermination
            case _: InterruptedException   =>
              unregisterMailboxAndActor(refId)
              InterruptedTermination
            case InstantiationException(e) =>
              unregisterMailboxAndActor(refId)
              InitializationTermination
            case _: CancellationException  =>
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
    *
    * @param timeoutMillis
    *   Maximum time to wait for actors to terminate in milliseconds
    * @return
    *   true if all actors terminated successfully, false if timeout occurred
    */
  private final def cleanUp(timeoutMillis: Long): Boolean = {
    val deadline = System.currentTimeMillis() + timeoutMillis

    // Keep trying until all actors are terminated or timeout
    while (actors.nonEmpty && System.currentTimeMillis() < deadline) {
      // Get snapshot of current actors to avoid concurrent modification
      val currentActors = actors.toMap

      // Send PoisonPill to all mailboxes
      mailboxes.values.foreach { mailbox =>
        mailbox.offer(PoisonPill) // Non-blocking to avoid deadlock
      }

      // Wait for actors to terminate with remaining time
      currentActors.foreach { case (_, actorRef) =>
        val remainingTime = deadline - System.currentTimeMillis()
        if (remainingTime > 0) {
          Try(actorRef.get(remainingTime, TimeUnit.MILLISECONDS))
        }
      }
    }

    actors.isEmpty
  }

  /** Shutdown the actor system and wait for all actors to terminate. All calls
    * to tell will throw an exception after this is called.
    *
    * @param timeoutMillis
    *   Maximum time to wait for shutdown in milliseconds (default: 30 seconds)
    * @return
    *   true if shutdown completed successfully, false if timeout occurred
    */
  final def shutdownAwait(timeoutMillis: Long = 30000): Boolean = {
    // Set shutdown flag first to prevent new actors from being created
    if (!isShuttingDown.compareAndSet(false, true)) {
      // Already shutting down
      return actors.isEmpty
    }

    cleanUp(timeoutMillis)
  }

  /** Tell an actor to process a message. If the actor does not exist, it will
    * be created.
    */
  final def tell[A <: Actor: ClassTag](name: String, msg: Any): Unit = {
    require(name != null && name.nonEmpty, "Actor name cannot be null or empty")
    require(msg != null, "Message cannot be null")

    if isShuttingDown.get() then
      throw new IllegalStateException("ActorSystem is shutting down")

    actorForName[A](name).put(msg)
  }

}

object ActorSystem {
  def apply(): ActorSystem = new ActorSystem {}
}
