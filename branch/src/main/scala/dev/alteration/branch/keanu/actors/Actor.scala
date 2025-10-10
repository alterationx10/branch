package dev.alteration.branch.keanu.actors

/** An actor trait which defines the onMsg method for handling messages.
  */
trait Actor extends Product with Serializable {

  /** Internal storage for actor context. Injected by ActorSystem.
    */
  private[actors] var _context: ActorContext = scala.compiletime.uninitialized

  /** The actor context - provides access to self, parent, children, and the
    * actor system.
    *
    * This is injected by the ActorSystem when the actor is created. Do not
    * access this field in the actor's constructor - it will be uninitialized
    * until after construction completes.
    */
  protected def context: ActorContext = _context

  /** The method to handle messages sent to the actor.
    */
  def onMsg: PartialFunction[Any, Any]

  /** The supervision strategy for this actor. Default is RestartStrategy.
    *
    * Override this to change how the actor behaves when it throws an exception
    * during message processing.
    */
  def supervisorStrategy: SupervisionStrategy = RestartStrategy

  /** Called before the actor starts processing messages.
    *
    * This is called when the actor is first created. Use this for
    * initialization like opening connections or acquiring resources.
    *
    * If this method throws an exception, the actor will be terminated according
    * to the supervision strategy.
    */
  def preStart(): Unit = ()

  /** Called after the actor stops processing messages.
    *
    * This is called when the actor terminates, whether due to a PoisonPill,
    * supervision decision, or system shutdown. Use this for cleanup like
    * closing connections or releasing resources.
    *
    * This method should not throw exceptions. If it does, they will be logged
    * but not propagated.
    */
  def postStop(): Unit = ()

  /** Called before the actor restarts due to an exception.
    *
    * This is called after the actor fails but before it is recreated. Use this
    * to clean up resources or save state before the restart.
    *
    * @param reason
    *   The exception that caused the restart
    */
  def preRestart(reason: Throwable): Unit = ()

  /** Called after the actor restarts due to an exception.
    *
    * This is called after the actor is recreated but before it processes new
    * messages. Use this to restore state or reinitialize resources.
    *
    * @param reason
    *   The exception that caused the restart
    */
  def postRestart(reason: Throwable): Unit = ()
}
