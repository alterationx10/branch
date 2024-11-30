package dev.wishingtree.branch.keanu.actors

import scala.reflect.{ClassTag, classTag}

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
    */
  def fromIdentifier(id: String): ActorRefId = {
    val (name: String) :: (propId: String) :: Nil =
      id.split(":").toList: @unchecked
    ActorRefId(name, propId)
  }
}
