package dev.wishingtree.branch.lzy

import scala.concurrent.Future
import scala.util.Try

sealed trait Lazy[+A] {
  final def flatMap[B](f: A => Lazy[B]): Lazy[B]              = Lazy.FlatMap(this, f)
  final def map[B](f: A => B): Lazy[B]                        = flatMap(a => Lazy.value(f(a)))
  final def recover[B >: A](f: Throwable => Lazy[B]): Lazy[B] =
    Lazy.Recover(this, f)

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
}

object Lazy {

  extension [A](lzy: Lazy[A]) {
    def runSync(): Try[A]     = LazyRuntime.runSync(lzy)
    def runAsync(): Future[A] = LazyRuntime.runAsync(lzy)
  }

  case class Fn[A](a: () => A)     extends Lazy[A]
  case class Fail[A](e: Throwable) extends Lazy[A]

  case class FlatMap[A, B](lzy: Lazy[A], f: A => Lazy[B])      extends Lazy[B]
  case class Recover[A](lzy: Lazy[A], f: Throwable => Lazy[A]) extends Lazy[A]

  def fn[A](a: => A): Lazy[A]                = Fn(() => a)
  def value[A](a: A): Lazy[A]                = Fn(() => a)
  def fail[A](throwable: Throwable): Lazy[A] = Fail(throwable)

  def forEach[A, B](xs: Iterable[A])(f: A => Lazy[B]): Lazy[Iterable[B]] =
    xs.foldLeft(Lazy.value(Vector.empty[B]))((acc, curr) => {
      for {
        soFar <- acc
        x     <- f(curr)
      } yield soFar :+ x
    })
}
