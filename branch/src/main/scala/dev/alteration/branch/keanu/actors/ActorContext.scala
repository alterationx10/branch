package dev.alteration.branch.keanu.actors

import scala.reflect.ClassTag

/** The context in which an actor executes.
  *
  * Provides access to the actor's identity, hierarchy, and the ability to
  * create and manage child actors.
  */
trait ActorContext {

  /** Reference to this actor.
    *
    * Use this to pass your own reference to other actors for reply-to patterns.
    *
    * @return
    *   This actor's ActorRefId
    */
  def self: ActorRefId

  /** Reference to the parent actor.
    *
    * @return
    *   Some(parent) if this actor has a parent, None for top-level actors
    */
  def parent: Option[ActorRefId]

  /** List of all child actors created by this actor.
    *
    * @return
    *   List of child ActorRefIds
    */
  def children: List[ActorRefId]

  /** Create a child actor under this actor.
    *
    * The child will be created at a path under this actor's path. For example,
    * if this actor is at /user/supervisor, creating a child named "worker1"
    * will create an actor at /user/supervisor/worker1.
    *
    * @param name
    *   The name of the child actor
    * @tparam A
    *   The actor type
    * @return
    *   The ActorRefId of the created child
    * @throws IllegalArgumentException
    *   if name is invalid
    * @throws IllegalStateException
    *   if the system is shutting down
    */
  def actorOf[A <: Actor: ClassTag](name: String): ActorRefId

  /** Stop a child actor.
    *
    * Sends a PoisonPill to the child actor, causing it to terminate gracefully.
    * The child must be a direct child of this actor.
    *
    * @param child
    *   The child actor to stop
    * @throws IllegalArgumentException
    *   if the actor is not a child of this actor
    */
  def stop(child: ActorRefId): Unit

  /** Access to the actor system.
    *
    * Provides access to system-level operations like top-level actor creation,
    * actor selection, etc.
    *
    * @return
    *   The ActorSystem
    */
  def system: ActorSystem
}

/** Internal implementation of ActorContext.
  *
  * This is injected into actors by the ActorSystem when they are created.
  */
private[actors] class ActorContextImpl(
    val self: ActorRefId,
    val system: ActorSystem
) extends ActorContext {

  override def parent: Option[ActorRefId] = {
    self.parent.flatMap(parentPath => system.actorSelection(parentPath))
  }

  override def children: List[ActorRefId] = {
    system.getChildren(self.path)
  }

  override def actorOf[A <: Actor: ClassTag](name: String): ActorRefId = {
    require(
      name != null && name.nonEmpty,
      "Child actor name cannot be null or empty"
    )
    require(!name.contains("/"), "Child actor name cannot contain '/'")

    val childPath = self.path / name
    system.actorOf[A](childPath)
  }

  override def stop(child: ActorRefId): Unit = {
    require(child != null, "Child actor cannot be null")

    // Verify this is actually a child
    if (!children.contains(child)) {
      throw new IllegalArgumentException(
        s"Actor ${child.path} is not a child of ${self.path}"
      )
    }

    system.tell(child, PoisonPill)
  }
}
