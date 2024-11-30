package dev.wishingtree.branch.lzy

import java.time.{Clock, Instant}
import java.util.logging.{Level, Logger}
import scala.annotation.targetName
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Try

sealed trait Lazy[+A] {

  /** FlatMap the result of a Lazy to a new Lazy value.
    */
  final def flatMap[B](f: A => Lazy[B]): Lazy[B] =
    Lazy.FlatMap(this, f)

  /** Map the result of a Lazy to a new Lazy value.
    */
  final def map[B](f: A => B): Lazy[B] =
    flatMap(a => Lazy.fn(f(a)))

  /** Flatten a nested Lazy */
  final def flatten[B](using ev: A <:< Lazy[B]) =
    this.flatMap(a => ev(a))

  /** If the Lazy fails, attempt to recover with the provided function.
    */
  final def recover[B >: A](f: Throwable => Lazy[B]): Lazy[B] =
    Lazy.Recover(this, f)

  /** If the Lazy fails, return the provided default Lazy value.
    */
  final def orElse[B >: A](default: => Lazy[B]): Lazy[B] =
    this.recover(_ => default)

  /** If the Lazy fails, return the provided default value.
    */
  final def orElseDefault[B >: A](default: B): Lazy[B] =
    this.orElse(Lazy.fn(default))

  /** Run the Lazy value forever, repeating the computation indefinitely.
    */
  final def forever[A] = {
    lazy val loop: Lazy[A] = this.flatMap(_ => loop)
    loop
  }

  /** Map the result of a Lazy to Unit */
  final def unit: Lazy[Unit] =
    this.map(_ => ())

  /** A symbolic [[flatMap]], where the value of the first lazy is ignored */
  @targetName("flatMapIgnore")
  final def *>[B](that: => Lazy[B]): Lazy[B] =
    this.flatMap(_ => that)

  /** Ignore the result on Lazy evaluation, and map it to the provided value */
  final def as[B](b: => B): Lazy[B] =
    this.map(_ => b)

  /** Ignore the result/failure of the Lazy value.
    */
  final def ignore: Lazy[Unit] =
    this.unit.recover(_ => Lazy.unit)

  /** Tap the Lazy value, evaluating the provided function, before continuing
    * with the original value. The result of the function is ignored.
    */
  final def tap(f: A => Unit): Lazy[A] = {
    this.flatMap(a => Lazy.fn(a).ignore.as(a))
  }

  /** Debug the Lazy value, printing it to the console on evaluation.
    */
  final def debug(prefix: String = "") =
    this.tap { a =>
      if prefix.isEmpty then println(s"$a")
      else println(s"$prefix: $a")
    }

  /** Retry the Lazy computation up to n times if it fails.
    */
  final def retryN(n: Int): Lazy[A] = {
    if n > 0 then this.recover(_ => this.retryN(n - 1))
    else this
  }

  /** Delay the Lazy for the provided duration before the original computation.
    */
  final def delay(duration: Duration): Lazy[A] =
    Lazy.sleep(duration).flatMap(_ => this)

  /** Pause the Lazy for the provided duration after the original computation,
    * then continue with the value.
    */
  final def pause(duration: Duration): Lazy[A] =
    this.flatMap(a => Lazy.sleep(duration).as(a))

  /** Map the error of the Lazy, evaluating the provided function.
    */
  final def mapError(f: Throwable => Throwable): Lazy[A] =
    this.recover(e => Lazy.fail(f(e)))

  /** Tap the failure of the Lazy, evaluating the provided function, before
    * re-failing with the original error. The result of the function is ignored.
    */
  final def tapError(f: Throwable => Unit): Lazy[A] =
    this.recover(e => Lazy.fn(f(e)).ignore.flatMap(_ => Lazy.fail(e)))

  /** Log the error at the SEVERE level.
    */
  final def logError(using logger: Logger): Lazy[A] =
    this.tapError(e => logger.log(Level.SEVERE, e.getMessage, e))
}

object Lazy {

  extension [A](lzy: Lazy[A]) {

    /** Run the Lazy value synchronously */
    def runSync(): Try[A] = LazyRuntime.runSync(lzy)

    /** Run the Lazy value asynchronously */
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

  /** A Lazy value that evaluates the provided expression.
    */
  def fn[A](a: => A): Lazy[A] = Fn(() => a)

  /** A Lazy value that fails with the provided throwable.
    */
  def fail[A](throwable: Throwable): Lazy[A] = Fail(throwable)

  /** A Lazy value that sleeps for the provided duration.
    */
  def sleep(duration: Duration): Lazy[Unit] = Sleep(duration)

  /** FoldLeft over the provided iterable, collecting the Lazy results */
  def forEach[A, B](xs: Iterable[A])(f: A => Lazy[B]): Lazy[Iterable[B]] =
    xs.foldLeft(Lazy.fn(Vector.empty[B]))((acc, curr) => {
      for {
        soFar <- acc
        x     <- f(curr)
      } yield soFar :+ x
    })

  /** A Lazy value that prints the provided string.
    */
  def println(str: String): Lazy[Unit] =
    fn(scala.Predef.println(str))

  /** A Lazy value that returns the current time using the provided Clock.
    * @param clock
    *   Default is Clock.systemUTC()
    */
  def now(clock: Clock = Clock.systemUTC()): Lazy[Instant] =
    fn(clock.instant())

  /** A Lazy value that does nothing.
    */
  def unit: Lazy[Unit] =
    Lazy.fn(())

  /** Log a message at the specified level.
    */
  def log(msg: String, level: Level)(using logger: Logger): Lazy[Unit] =
    Lazy.fn(logger.log(level, msg))

  /** Log a message at the specified level.
    */
  def log[A](a: A, level: Level)(using
      logger: Logger,
      conversion: Conversion[A, String]
  ): Lazy[Unit] =
    Lazy.fn(logger.log(level, conversion(a)))

  /** Log a message at the specified level.
    */
  def log(msg: String, level: Level, e: Throwable)(using
      logger: Logger
  ): Lazy[Unit] =
    Lazy.fn(logger.log(level, msg, e))

  /** Log a message at the specified level.
    */
  def log[A](a: A, level: Level, e: Throwable)(using
      logger: Logger,
      conversion: Conversion[A, String]
  ): Lazy[Unit] =
    Lazy.fn(logger.log(level, conversion(a), e))

  /** Log a message at the SEVERE level.
    */
  def logSevere(msg: String, e: Throwable)(using logger: Logger): Lazy[Unit] =
    log(msg, Level.SEVERE, e)

  /** Log a message at the SEVERE level.
    */
  def logSevere[A](a: A, e: Throwable)(using
      logger: Logger,
      conversion: Conversion[A, String]
  ): Lazy[Unit] =
    log(a, Level.SEVERE, e)

  /** Log a message at the SEVERE level.
    */
  def logWarning(msg: String)(using logger: Logger): Lazy[Unit] =
    log(msg, Level.WARNING)

  /** Log a message at the WARNING level.
    */
  def logWarning[A](a: A)(using
      logger: Logger,
      conversion: Conversion[A, String]
  ): Lazy[Unit] =
    log(a, Level.WARNING)

  /** Log a message at the INFO level.
    */
  def logInfo(msg: String)(using logger: Logger): Lazy[Unit] =
    log(msg, Level.INFO)

  /** Log a message at the INFO level.
    */
  def logInfo[A](a: A)(using
      logger: Logger,
      conversion: Conversion[A, String]
  ): Lazy[Unit] =
    log(a, Level.INFO)

  /** Log a message at the CONFIG level.
    */
  def logConfig(msg: String)(using logger: Logger): Lazy[Unit] =
    log(msg, Level.CONFIG)

  /** Log a message at the CONFIG level.
    */
  def logConfig[A](a: A)(using
      logger: Logger,
      conversion: Conversion[A, String]
  ): Lazy[Unit] =
    log(a, Level.CONFIG)

  /** Log a message at the FINE level.
    */
  def logFine(msg: String)(using logger: Logger): Lazy[Unit] =
    log(msg, Level.FINE)

  /** Log a message at the FINE level.
    */
  def logFine[A](a: A)(using
      logger: Logger,
      conversion: Conversion[A, String]
  ): Lazy[Unit] =
    log(a, Level.FINE)

}
