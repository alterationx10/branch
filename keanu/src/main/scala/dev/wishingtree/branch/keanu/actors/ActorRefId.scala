package dev.wishingtree.branch.keanu.actors

import scala.reflect.{ClassTag, classTag}

case class ActorRefId(name: String, propId: String)

object ActorRefId {
  def apply[A <: Actor: ClassTag](name: String): ActorRefId =
    ActorRefId(name, classTag[A].getClass.getCanonicalName)
}
