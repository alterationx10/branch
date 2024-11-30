package dev.wishingtree.branch.keanu.actors

import scala.deriving.Mirror
import scala.reflect.{ClassTag, classTag}

/** A type-class for creating actors.
  */
trait ActorProps[A <: Actor] {
  private[actors] val identifier: String

  /** A method to create an instance of an Actor */
  def create(): A
}

object ActorProps {

  /** Creates an ActorProps from a type argument, and a product of arguments.
    * The arguments must match the constructor of the actor, which is checked at
    * compile time.
    */
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
