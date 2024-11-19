package dev.wishingtree.branch.keanu.actors

import scala.reflect.{ClassTag, classTag}

trait ActorContext[A <: Actor: ClassTag] {
  private[actors] val ctorArgs: Seq[Any]
  private[actors] val identifier  =
    classTag[A].runtimeClass.getCanonicalName
  private[actors] def create(): A = {
    try {
      classTag[A].runtimeClass.getConstructors.head
        .newInstance(ctorArgs*)
        .asInstanceOf[A]
    } catch {
      case e: Throwable => throw InstantiationException(e)
    }
  }
}

object ActorContext {
  def props[A <: Actor: ClassTag](args: Any*): ActorContext[A] = {
    new ActorContext[A] {
      override private[actors] val ctorArgs: Seq[Any] = args
    }
  }
}
