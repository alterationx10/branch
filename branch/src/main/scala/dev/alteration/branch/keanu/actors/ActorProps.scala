package dev.alteration.branch.keanu.actors

import scala.deriving.Mirror
import scala.reflect.ClassTag

/** A type-class for creating actors.
  */
trait ActorProps[A <: Actor] {
  private[actors] val identifier: String

  /** The mailbox type for this actor */
  val mailboxType: MailboxType

  /** A method to create an instance of an Actor */
  def create(): A
}

object ActorProps {

  protected class DerivedActorProps[A <: Actor](
      args: Product,
      override val mailboxType: MailboxType
  )(using
      m: Mirror.ProductOf[A],
      ct: ClassTag[A]
  ) extends ActorProps[A] {
    override private[actors] val identifier = ct.runtimeClass.getName
    override def create(): A                = m.fromProduct(args)
  }

  /** Creates an ActorProps from a type argument, and a product of arguments.
    * The arguments must match the constructor of the actor, which is checked at
    * compile time.
    *
    * @param args
    *   Constructor arguments for the actor
    * @param mailboxType
    *   The mailbox type to use (default: UnboundedMailbox)
    */
  inline def props[A <: Actor: ClassTag](
      args: Product,
      mailboxType: MailboxType = UnboundedMailbox
  )(using
      m: Mirror.ProductOf[A],
      ev: args.type <:< m.MirroredElemTypes
  ): ActorProps[A] = new DerivedActorProps[A](args, mailboxType)

}
