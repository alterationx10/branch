package dev.alteration.branch.keanu.actors

import dev.alteration.branch.macaroni.runtimes.BranchExecutors
import dev.alteration.branch.keanu

import java.time.Instant
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.*
import scala.concurrent.duration.*
import scala.concurrent.{Future, Promise}
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag
import scala.util.*

/** The default ActorSystem implementation. For singleton usage, have an object
  * extend this trait. For an instance usage, the default apply method can be
  * used.
  */
trait ActorSystem {

  /** The logger used for actor system events. Override this to provide custom
    * logging.
    */
  def logger: ActorLogger = NoOpLogger

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

  /** Backoff state for actors using RestartWithBackoff strategy
    */
  private case class BackoffState(
      failureCount: Int,
      lastFailureTime: Long,
      currentBackoff: Long
  )

  private final val backoffState: concurrent.Map[ActorRefId, BackoffState] =
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

  /** Adds a dead letter to the queue. If the queue is full, the oldest entry is
    * silently dropped.
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
    * @param error
    *   Optional error that caused the restart
    * @param strategy
    *   The supervision strategy to apply
    */
  private final def restartActor(
      refId: ActorRefId,
      error: Option[Throwable] = None,
      strategy: SupervisionStrategy = RestartStrategy
  ): Unit = {
    val mailbox = getOrCreateMailbox(refId)
    actors -= refId

    // Handle backoff if using RestartWithBackoff strategy
    strategy match {
      case RestartWithBackoff(minBackoff, maxBackoff, maxRetries, resetAfter) =>
        val now   = System.currentTimeMillis()
        val state = backoffState.get(refId)

        // Check if we should reset the failure count
        val resetState = state.flatMap { s =>
          resetAfter.flatMap { reset =>
            if (now - s.lastFailureTime > reset.toMillis) {
              Some(BackoffState(0, now, minBackoff.toMillis))
            } else None
          }
        }

        val currentState = resetState
          .orElse(state)
          .getOrElse(
            BackoffState(0, now, minBackoff.toMillis)
          )

        val newFailureCount = currentState.failureCount + 1

        // Check if we exceeded max retries (retries = restarts after initial failure)
        // So maxRetries = 3 means: initial failure + 3 restarts = 4 total attempts
        if (maxRetries.exists(newFailureCount >= _)) {
          unregisterMailboxAndActor(refId)
          backoffState -= refId
          return
        }

        // Calculate next backoff (exponential with max)
        val nextBackoff = math.min(
          currentState.currentBackoff * 2,
          maxBackoff.toMillis
        )

        backoffState += (refId -> BackoffState(
          newFailureCount,
          now,
          nextBackoff
        ))

        // Wait before restarting
        Thread.sleep(currentState.currentBackoff)

      case _ => () // No backoff for other strategies
    }

    actors += (refId -> submitActor(refId, mailbox, error))
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
    require(
      prop.identifier != null && prop.identifier.nonEmpty,
      "ActorProps identifier cannot be null or empty"
    )
    props += (prop.identifier -> prop)
  }

  /** Get or create a mailbox for the given refId
    * @param refId
    * @return
    */
  private final def getOrCreateMailbox(
      refId: ActorRefId
  ): Mailbox =
    mailboxes.getOrElseUpdate(
      refId,
      props.get(refId.propId) match {
        case Some(prop) => prop.mailboxType.createMailbox()
        case None       => new LinkedBlockingQueue[Any]() // Fallback
      }
    )

  /** Submit an actor to the executor service
    * @param refId
    * @param mailbox
    * @param restartReason
    *   If this is a restart, the error that caused it
    * @return
    */
  private final def submitActor(
      refId: ActorRefId,
      mailbox: Mailbox,
      restartReason: Option[Throwable] = None
  ): ActorRef = {
    CompletableFuture.supplyAsync[LifecycleEvent](
      () => {
        var currentActor: Option[Actor]       = None
        val terminationResult: LifecycleEvent = {
          try {
            val newActor: Actor =
              try {
                props(refId.propId).create()
              } catch {
                case e: Exception => throw InstantiationException(e)
              }
            currentActor = Some(newActor) // Save for catch block

            // Call lifecycle hooks
            try {
              restartReason match {
                case Some(error) =>
                  // This is a restart
                  logger.lifecycleEvent(ActorRestarted(error), refId)
                  try {
                    newActor.postRestart(error)
                  } catch {
                    case _: Exception => () // Log but don't fail
                  }
                case None        =>
                  // This is initial start
                  logger.lifecycleEvent(ActorStarted, refId)
                  newActor.preStart()
              }
            } catch {
              case e: Exception =>
                // preStart/postRestart failed, terminate actor
                try {
                  newActor.postStop()
                } catch {
                  case _: Exception => () // Ignore exceptions in postStop
                }
                throw InstantiationException(e)
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
            case PoisonPillException =>
              // Call postStop before terminating
              logger.lifecycleEvent(ActorStopped, refId)
              val actor = Try(props(refId.propId).create()).toOption
              actor.foreach { a =>
                try {
                  a.postStop()
                } catch {
                  case _: Exception => () // Ignore exceptions in postStop
                }
              }
              unregisterMailboxAndActor(refId)
              backoffState -= refId
              PoisonPillTermination

            case _: InterruptedException =>
              logger.lifecycleEvent(ActorTerminated, refId)
              unregisterMailboxAndActor(refId)
              backoffState -= refId
              InterruptedTermination

            case InstantiationException(e) =>
              logger.error("Actor instantiation failed", Some(refId), Some(e))
              logger.lifecycleEvent(ActorTerminated, refId)
              unregisterMailboxAndActor(refId)
              backoffState -= refId
              InitializationTermination

            case _: CancellationException =>
              logger.lifecycleEvent(ActorTerminated, refId)
              unregisterMailboxAndActor(refId)
              backoffState -= refId
              CancellationTermination

            case e: Throwable =>
              // Check supervision strategy of the current actor
              logger.lifecycleEvent(ActorFailed(e), refId)
              currentActor.foreach { actor =>
                // Call preRestart before deciding
                try {
                  actor.preRestart(e)
                } catch {
                  case _: Exception => () // Ignore exceptions in preRestart
                }

                actor.supervisorStrategy match {
                  case StopStrategy =>
                    // Stop the actor
                    logger.lifecycleEvent(ActorStopped, refId)
                    try {
                      actor.postStop()
                    } catch {
                      case _: Exception => () // Ignore exceptions in postStop
                    }
                    unregisterMailboxAndActor(refId)
                    backoffState -= refId

                  case strategy @ (RestartStrategy | _: RestartWithBackoff) =>
                    // Restart the actor
                    logger.debug(
                      s"Restarting actor with strategy: ${strategy.getClass.getSimpleName}",
                      Some(refId)
                    )
                    restartActor(refId, Some(e), strategy)
                }
              }

              // If actor wasn't created, stop it
              if (currentActor.isEmpty) {
                logger.lifecycleEvent(ActorTerminated, refId)
                unregisterMailboxAndActor(refId)
                backoffState -= refId
              }

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

    logger.info("ActorSystem shutting down")
    val result = cleanUp(timeoutMillis)
    if (result) {
      logger.info("ActorSystem shutdown completed successfully")
    } else {
      logger.warn("ActorSystem shutdown timed out")
    }
    result
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

  /** Ask an actor to process a message and return a Future with the response.
    *
    * The actor must handle Ask messages and complete the promise with the
    * result. If the actor doesn't respond within the timeout, the Future will
    * fail with AskTimeoutException.
    *
    * Example:
    * {{{
    * case class QueryActor() extends Actor {
    *   def onMsg: PartialFunction[Any, Any] = {
    *     case ask: Ask[?] =>
    *       ask.message match {
    *         case query: String => ask.complete(processQuery(query))
    *       }
    *   }
    * }
    *
    * val system = ActorSystem()
    * val future = system.ask[QueryActor, String]("myActor", "query", 5.seconds)
    * future.onComplete {
    *   case Success(result) => println(result)
    *   case Failure(e) => println(s"Failed: $e")
    * }
    * }}}
    *
    * @param name
    *   The name of the actor to ask
    * @param msg
    *   The message to send
    * @param timeout
    *   Maximum time to wait for a response (default: 5 seconds)
    * @tparam A
    *   The actor type
    * @tparam R
    *   The expected response type
    * @return
    *   A Future that completes with the response or fails with
    *   AskTimeoutException
    */
  final def ask[A <: Actor: ClassTag, R](
      name: String,
      msg: Any,
      timeout: Duration = 5.seconds
  ): Future[R] = {
    require(name != null && name.nonEmpty, "Actor name cannot be null or empty")
    require(msg != null, "Message cannot be null")
    require(
      timeout.isFinite && timeout > Duration.Zero,
      "Timeout must be positive and finite"
    )

    if isShuttingDown.get() then
      throw new IllegalStateException("ActorSystem is shutting down")

    val promise = Promise[R]()
    val ask     = Ask[R](msg, promise)

    // Send the wrapped message
    actorForName[A](name).put(ask)

    // Create a timeout task
    val timeoutTask = new Runnable {
      def run(): Unit = {
        promise.tryFailure(
          new AskTimeoutException(
            s"Ask timeout after ${timeout} for actor '$name' with message: $msg",
            name,
            msg
          )
        )
      }
    }

    // Schedule the timeout
    val timeoutFuture = new CompletableFuture[Unit]()
    CompletableFuture
      .delayedExecutor(
        timeout.toMillis,
        TimeUnit.MILLISECONDS,
        executorService
      )
      .execute(() => {
        timeoutTask.run()
        timeoutFuture.complete(())
      })

    // Return the promise's future
    promise.future
  }

}

object ActorSystem {
  def apply(): ActorSystem = new ActorSystem {}
}
