package dev.wishingtree.branch.lzy

import java.time.{Clock, Instant}
import java.util.logging.{Level, Logger}
import scala.annotation.targetName
import scala.concurrent.Future
import scala.concurrent.duration.Duration
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

  final def orElseDefault[B >: A](default: B): Lazy[B] =
    this.orElse(Lazy.fn(default))

  final def forever[A] = {
    lazy val loop: Lazy[A] = this.flatMap(_ => loop)
    loop
  }

  final def unit: Lazy[Unit] =
    this.map(_ => ())

  @targetName("flatMapIgnore")
  final def *>[B](that: => Lazy[B]): Lazy[B] =
    this.flatMap(_ => that)

  final def as[B](b: => B): Lazy[B] =
    this.map(_ => b)

  final def ignore: Lazy[Unit] =
    this.unit.recover(_ => Lazy.unit)

  final def tap(f: A => Unit): Lazy[A] = {
    this.flatMap(a => Lazy.fn(a).ignore.as(a))
  }

  final def debug(prefix: String = "") =
    this.tap { a =>
      if prefix.isEmpty then println(s"$a")
      else println(s"$prefix: $a")
    }

  final def retryN(n: Int): Lazy[A] = {
    if n > 0 then this.recover(_ => this.retryN(n - 1))
    else this
  }

  final def delay(duration: Duration): Lazy[A] =
    Lazy.sleep(duration).flatMap(_ => this)

  final def pause(duration: Duration): Lazy[A] =
    this.flatMap(a => Lazy.sleep(duration).as(a))

  final def mapError(f: Throwable => Throwable): Lazy[A] =
    this.recover(e => Lazy.fail(f(e)))

  final def tapError(f: Throwable => Unit): Lazy[A] =
    this.recover(e => Lazy.fn(f(e)).flatMap(_ => Lazy.fail(e)))

  final def logError(using logger: Logger): Lazy[A] =
    this.tapError(e => logger.log(Level.SEVERE, e.getMessage, e))
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

  private[lzy] final case class Sleep(duration: Duration) extends Lazy[Unit]

  def fn[A](a: => A): Lazy[A]                = Fn(() => a)
  def fail[A](throwable: Throwable): Lazy[A] = Fail(throwable)
  def sleep(duration: Duration): Lazy[Unit]  = Sleep(duration)

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

  def log(msg: String, level: Level)(using logger: Logger): Lazy[Unit] =
    Lazy.fn(logger.log(level, msg))

  def log[A](a: A, level: Level)(using
      logger: Logger,
      conversion: Conversion[A, String]
  ): Lazy[Unit] =
    Lazy.fn(logger.log(level, conversion(a)))

  def log(msg: String, level: Level, e: Throwable)(using
      logger: Logger
  ): Lazy[Unit] =
    Lazy.fn(logger.log(level, msg, e))

  def log[A](a: A, level: Level, e: Throwable)(using
      logger: Logger,
      conversion: Conversion[A, String]
  ): Lazy[Unit] =
    Lazy.fn(logger.log(level, conversion(a), e))

  def logSevere(msg: String, e: Throwable)(using logger: Logger): Lazy[Unit] =
    log(msg, Level.SEVERE, e)

  def logSevere[A](a: A, e: Throwable)(using
      logger: Logger,
      conversion: Conversion[A, String]
  ): Lazy[Unit] =
    log(a, Level.SEVERE, e)

  def logWarning(msg: String)(using logger: Logger): Lazy[Unit] =
    log(msg, Level.WARNING)

  def logWarning[A](a: A)(using
      logger: Logger,
      conversion: Conversion[A, String]
  ): Lazy[Unit] =
    log(a, Level.WARNING)

  def logInfo(msg: String)(using logger: Logger): Lazy[Unit] =
    log(msg, Level.INFO)

  def logInfo[A](a: A)(using
      logger: Logger,
      conversion: Conversion[A, String]
  ): Lazy[Unit] =
    log(a, Level.INFO)

  def logConfig(msg: String)(using logger: Logger): Lazy[Unit] =
    log(msg, Level.CONFIG)

  def logConfig[A](a: A)(using
      logger: Logger,
      conversion: Conversion[A, String]
  ): Lazy[Unit] =
    log(a, Level.CONFIG)

  def logFine(msg: String)(using logger: Logger): Lazy[Unit] =
    log(msg, Level.FINE)

  def logFine[A](a: A)(using
      logger: Logger,
      conversion: Conversion[A, String]
  ): Lazy[Unit] =
    log(a, Level.FINE)

}
