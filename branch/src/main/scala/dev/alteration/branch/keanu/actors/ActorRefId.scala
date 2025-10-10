package dev.alteration.branch.keanu.actors

import scala.reflect.{classTag, ClassTag}

/** An internal identifier for an actor reference.
  *
  * @param path
  *   The hierarchical path to the actor
  * @param propId
  *   The actor's class name (used for type tracking)
  */
private[actors] case class ActorRefId(path: ActorPath, propId: String) {

  /** Returns the actor's name (last segment of the path).
    */
  def name: String = path.name

  /** Returns a string representation of the identifier.
    */
  lazy val toIdentifier: String = s"${path}:$propId"

  /** Returns the parent path if this actor has a parent.
    */
  def parent: Option[ActorPath] = path.parent
}

private[actors] object ActorRefId {

  /** Creates an ActorRefId from a type argument and name at the top-level /user
    * path.
    *
    * @param name
    *   The actor name
    * @tparam A
    *   The actor type
    * @return
    *   An ActorRefId under /user
    */
  def apply[A <: Actor: ClassTag](name: String): ActorRefId =
    ActorRefId(ActorPath.user(name), classTag[A].runtimeClass.getName)

  /** Creates an ActorRefId from a type argument and path.
    *
    * @param path
    *   The actor path
    * @tparam A
    *   The actor type
    * @return
    *   An ActorRefId with the given path
    */
  def apply[A <: Actor: ClassTag](path: ActorPath): ActorRefId =
    ActorRefId(path, classTag[A].runtimeClass.getName)

  /** Re-create an ActorRefId from an identifier. Returns None if the identifier
    * format is invalid.
    */
  def fromIdentifier(id: String): Option[ActorRefId] = {
    id.split(":").toList match {
      case pathStr :: propId :: Nil =>
        ActorPath.fromString(pathStr).map(ActorRefId(_, propId))
      case _                        => None
    }
  }
}
