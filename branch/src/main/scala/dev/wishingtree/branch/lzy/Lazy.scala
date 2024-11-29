package dev.wishingtree.branch.lzy

import java.time.{Clock, Instant}
import scala.annotation.targetName
import scala.concurrent.Future
import scala.util.Try

sealed trait Lazy[+A] {

  final def flatMap[B](f: A => Lazy[B]): Lazy[B] =
    Lazy.FlatMap(this, f)

  final def map[B](f: A => B): Lazy[B] =
    flatMap(a => Lazy.fn(f(a)))

  final def flatten[B](using ev: A <:< Lazy[B]) =
    this.flatMap(a => ev(a))

  final def recover[B >: A](f: Throwable => Lazy[B]): Lazy[B] =
    Lazy.Recover(this, f)

  final def orElse[B >: A](default: => Lazy[B]): Lazy[B] =
    this.recover(_ => default)

  final def orElseValue[B >: A](default: B): Lazy[B] =
    this.orElse(Lazy.fn(default))

  final def forever[A] = {
    lazy val loop: Lazy[A] = this.flatMap(_ => loop)
    loop
  }

  final def debug(prefix: String = "") =
    this.map { a =>
      if prefix.isEmpty then println(s"$a")
      else println(s"$prefix: $a")
      a
    }

  final def unit: Lazy[Unit] =
    this.map(_ => ())

  @targetName("flatMapIgnore")
  final def *>[B](that: => Lazy[B]): Lazy[B] =
    this.flatMap(_ => that)

  def as[B](b: => B): Lazy[B] =
    this.map(_ => b)

  def ignore: Lazy[Unit] =
    this.unit.recover(_ => Lazy.unit)
}

object Lazy {

  extension [A](lzy: Lazy[A]) {
    def runSync(): Try[A]     = LazyRuntime.runSync(lzy)
    def runAsync(): Future[A] = LazyRuntime.runAsync(lzy)
  }

  private[lzy] final case class Fn[A](a: () => A)     extends Lazy[A]
  private[lzy] final case class Fail[A](e: Throwable) extends Lazy[A]

  private[lzy] final case class FlatMap[A, B](lzy: Lazy[A], f: A => Lazy[B])
      extends Lazy[B]
  private[lzy] final case class Recover[A](
      lzy: Lazy[A],
      f: Throwable => Lazy[A]
  ) extends Lazy[A]

  def fn[A](a: => A): Lazy[A]                = Fn(() => a)
  def fail[A](throwable: Throwable): Lazy[A] = Fail(throwable)

  def forEach[A, B](xs: Iterable[A])(f: A => Lazy[B]): Lazy[Iterable[B]] =
    xs.foldLeft(Lazy.fn(Vector.empty[B]))((acc, curr) => {
      for {
        soFar <- acc
        x     <- f(curr)
      } yield soFar :+ x
    })

  def println(str: String): Lazy[Unit] =
    fn(scala.Predef.println(str))

  def now(clock: Clock = Clock.systemUTC()): Lazy[Instant] =
    fn(clock.instant())

  def unit: Lazy[Unit] =
    Lazy.fn(())
}
