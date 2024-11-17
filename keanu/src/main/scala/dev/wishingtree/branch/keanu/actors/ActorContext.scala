package dev.wishingtree.branch.keanu.actors

import scala.reflect.{ClassTag, classTag}

trait ActorContext[A <: Actor: ClassTag] {
  private[actors] val ctorArgs: Seq[Any]
  private[actors] val identifier  =
    classTag[A].getClass.getCanonicalName
  private[actors] def create(): A = {
    classTag[A].runtimeClass.getConstructors.head
      .newInstance(ctorArgs*)
      .asInstanceOf[A]
  }
}

object ActorContext {
  inline def props[A <: Actor: ClassTag](args: Any*): ActorContext[A] = {
    new ActorContext[A] {
      override private[actors] val ctorArgs: Seq[Any] = args
    }
  }
}
