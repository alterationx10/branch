package dev.alteration.branch.keanu.actors

import scala.reflect.{classTag, ClassTag}

/** An internal identifier for an actor reference.
  */
private[actors] case class ActorRefId(name: String, propId: String) {

  /** Returns a string representation of the identifier.
    */
  lazy val toIdentifier: String = s"$name:$propId"
}

private[actors] object ActorRefId {

  /** Creates an ActorRefId from a type argument and name.
    */
  def apply[A <: Actor: ClassTag](name: String): ActorRefId =
    ActorRefId(name, classTag[A].runtimeClass.getName)

  /** Re-create an ActorRefId from an identifier.
    * Returns None if the identifier format is invalid.
    */
  def fromIdentifier(id: String): Option[ActorRefId] = {
    id.split(":").toList match {
      case name :: propId :: Nil => Some(ActorRefId(name, propId))
      case _ => None
    }
  }
}
