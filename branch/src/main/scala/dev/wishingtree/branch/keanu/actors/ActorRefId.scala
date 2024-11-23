package dev.wishingtree.branch.keanu.actors

import scala.reflect.{ClassTag, classTag}

private[actors] case class ActorRefId(name: String, propId: String) {
  lazy val toIdentifier: String = s"$name:$propId"
}

private[actors] object ActorRefId {
  def apply[A <: Actor: ClassTag](name: String): ActorRefId =
    ActorRefId(name, classTag[A].runtimeClass.getName)

  def fromIdentifier(id: String): ActorRefId = {
    val (name: String) :: (propId: String) :: Nil =
      id.split(":").toList: @unchecked
    ActorRefId(name, propId)
  }
}
