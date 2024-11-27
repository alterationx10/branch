package dev.wishingtree.branch.keanu.actors

import scala.deriving.Mirror
import scala.reflect.{ClassTag, classTag}

trait ActorProps[A <: Actor] {
  private[actors] val identifier: String
  def create(): A
}

object ActorProps {

  inline def props[A <: Actor: ClassTag](args: Product)(using
      m: Mirror.ProductOf[A],
      ev: args.type <:< m.MirroredElemTypes
  ): ActorProps[A] =
    new ActorProps[A] {
      override private[actors] val identifier =
        classTag[A].runtimeClass.getName

      val _args: Product =
        args

      override def create(): A =
        m.fromProduct(_args)
    }

}
