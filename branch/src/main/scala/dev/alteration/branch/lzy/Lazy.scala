package dev.alteration.branch.lzy

import java.time.{Clock, Instant}
import java.util.logging.{Level, Logger}
import scala.annotation.targetName
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Using.Releasable
import scala.util.{Try, Using}

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
  final def flatten[B](using ev: A <:< Lazy[B]): Lazy[B] =
    this.flatMap(a => ev(a))

  /** Zip two Lazy values into a tuple.
    */
  final def zip[B](that: Lazy[B]): Lazy[(A, B)] =
    this.flatMap(a => that.map(b => (a, b)))

  /** Zip two Lazy values and apply a function to the results.
    */
  final def zipWith[B, C](that: Lazy[B])(f: (A, B) => C): Lazy[C] =
    this.zip(that).map { case (a, b) => f(a, b) }

  /** Race this Lazy against another, returning whichever completes first.
    *
    * Both computations are started concurrently. The result is a union type
    * A | B - you can pattern match on the type to determine which completed
    * first. If both fail, the first failure is returned.
    *
    * Example:
    * {{{
    *   val result: Int | String = Lazy.fn(42).race(Lazy.fn("hello")).runSync.get
    *   result match {
    *     case i: Int    => println(s"Left won: $i")
    *     case s: String => println(s"Right won: $s")
    *   }
    * }}}
    */
  final def race[B](that: Lazy[B]): Lazy[A | B] =
    Lazy.Race(this, that)

  /** If the Lazy fails, attempt to recover with the provided function.
    */
  final def recover[B >: A](f: Throwable => Lazy[B]): Lazy[B] =
    Lazy.Recover(this, f)

  final def recoverSome[B >: A](
      f: PartialFunction[Throwable, Lazy[B]]
  ): Lazy[B] =
    this.recover(f.applyOrElse(_, Lazy.fail))

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
  final def forever: Lazy[A] = {
    lazy val loop: Lazy[A] = this.flatMap(_ => loop)
    loop
  }

  /** Run the Lazy value until the result matches the provided condition is
    * true.
    */
  final def until(cond: A => Boolean): Lazy[A] = {
    lazy val loop: Lazy[A] = this.flatMap { a =>
      if (cond(a)) Lazy.fn(a)
      else loop
    }
    loop
  }

  /** Run the Lazy value until the provided condition is true
    */
  final def until(cond: => Boolean): Lazy[A] = {
    lazy val loop: Lazy[A] = this.flatMap { a =>
      if (cond) Lazy.fn(a)
      else loop
    }
    loop
  }

  /** Map the result of a Lazy to Unit */
  final def unit: Lazy[Unit] =
    this.map(_ => ())

  /** A symbolic [[flatMap]], where the value of the first lazy is ignored */
  @targetName("flatMapIgnore")
  final def *>[B](that: => Lazy[B]): Lazy[B] =
    this.flatMap(_ => that)

  /** Ignore the result on Lazy evaluation and map it to the provided value */
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
    this.flatMap(a => Lazy.fn(f(a)).ignore.as(a))
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

  /** Timeout the Lazy computation if it exceeds the provided duration.
    */
  final def timeout(duration: Duration): Lazy[A] =
    Lazy.Timeout(this, duration)

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

  /** Convert errors to values, returning Either[Throwable, A]. Success becomes
    * Right(a), failure becomes Left(throwable).
    */
  final def either: Lazy[Either[Throwable, A]] =
    this.map(a => Right(a)).recover(e => Lazy.fn(Left(e)))

  /** Wraps this Lazy in [[Lazy.when]]
    */
  final def when(cond: => Boolean): Lazy[Option[A]] =
    Lazy.when(cond)(this)

  /** Wraps the success of this Lazy into a Some, or None on failure.
    */
  final def optional: Lazy[Option[A]] =
    this.map(Some(_)).orElseDefault(None)

  /** Converts a Lazy[Option[A]] into a Lazy[A], using the default value if the
    * Option is empty
    */
  final def someOrElse[B](default: => B)(using ev: A <:< Option[B]): Lazy[B] =
    this.map(o => ev(o).getOrElse(default))

  /** Converts a Lazy[Option[A]] into a Lazy[A], failing with the provided
    * throwable if the Option is empty
    */
  final def someOrFail[B](throwable: => Throwable)(using
      ev: A <:< Option[B]
  ): Lazy[B] =
    this.flatMap(o => Lazy.fromOption(ev(o)).mapError(_ => throwable))
}

object Lazy {

  extension [A](lzy: Lazy[A]) {

    /** Run the Lazy value synchronously */
    def runSync(d: Duration = Duration.Inf)(using
        executionContext: ExecutionContext
    ): Try[A] =
      LazyRuntime.runSync(lzy, d)

    /** Run the Lazy value asynchronously */
    def runAsync(using executionContext: ExecutionContext): Future[A] =
      LazyRuntime.runAsync(lzy)
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

  private[lzy] final case class Timeout[A](lzy: Lazy[A], duration: Duration)
      extends Lazy[A]

  private[lzy] final case class FromFuture[A](f: Future[A]) extends Lazy[A]

  private[lzy] final case class Race[A, B](left: Lazy[A], right: Lazy[B])
      extends Lazy[A | B]

  /** A Lazy value that evaluates the provided expression.
    */
  def fn[A](a: => A): Lazy[A] = Fn(() => a)

  /** A Lazy value that fails with the provided throwable.
    */
  def fail[A](throwable: Throwable): Lazy[A] = Fail(throwable)

  /** A Lazy value that sleeps for the provided duration.
    */
  def sleep(duration: Duration): Lazy[Unit] = Sleep(duration)

  /** FoldLeft over the provided iterator, adding the Lazy fn result to the
    * provided builder, and returning the builder result.
    *
    * This is a "lower level" method used for concrete implementations of
    * forEach, but could be useful in other contexts.
    */
  def iterate[A, B, F[_]](
      xs: Iterator[A]
  )(xb: mutable.Builder[B, F[B]])(fn: A => Lazy[B]): Lazy[F[B]] = {
    xs.foldLeft(Lazy.fn(xb))((acc, curr) => {
      acc.flatMap(b => fn(curr).map(b.addOne))
    }).as(xb.result())
  }

  /** Iterate through the given Set, collecting the results of the Lazy f into a
    * new Set
    */
  def forEach[A, B](set: Set[A])(f: A => Lazy[B]): Lazy[Set[B]] =
    iterate(set.iterator)(Set.newBuilder[B])(f)

  /** Iterate through the given List, collecting the results of the Lazy f into
    * a new List
    */
  def forEach[A, B](list: List[A])(f: A => Lazy[B]): Lazy[List[B]] =
    iterate(list.iterator)(List.newBuilder[B])(f)

  /** Iterate through the given Seq, collecting the results of the Lazy f into a
    * new Seq
    */
  def forEach[A, B](seq: Seq[A])(f: A => Lazy[B]): Lazy[Seq[B]] =
    iterate(seq.iterator)(Seq.newBuilder[B])(f)

  /** Iterate through the given IndexedSeq, collecting the results of the Lazy f
    * into a new IndexedSeq
    */
  def forEach[A, B](seq: IndexedSeq[A])(f: A => Lazy[B]): Lazy[IndexedSeq[B]] =
    iterate(seq.iterator)(IndexedSeq.newBuilder[B])(f)

  /** Iterate through the given Vector, collecting the results of the Lazy f
    * into a new Vector
    */
  def forEach[A, B](vec: Vector[A])(f: A => Lazy[B]): Lazy[Vector[B]] =
    iterate(vec.iterator)(Vector.newBuilder[B])(f)

  /** Iterate through the given Array, collecting the results of the Lazy f into
    * a new Array
    */
  def forEach[A, B: ClassTag](arr: Array[A])(f: A => Lazy[B]): Lazy[Array[B]] =
    iterate(arr.iterator)(Array.newBuilder[B])(f)

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

  /** Lazily evaluates the Try, folding the failure into a Lazy.fail */
  def fromTry[A](t: => Try[A]): Lazy[A] =
    Lazy.fn(t.fold(Lazy.fail, Lazy.fn)).flatten

  /** Lifts a Future into a Lazy value.
    *
    * Important: The parameter is call-by-name, so the Future creation is
    * deferred until the Lazy is run. However, once created, Futures are eager
    * and will execute immediately.
    *
    * Example:
    * {{{
    *   // Future creation deferred - runs when Lazy executes
    *   Lazy.fromFuture(Future { expensiveComputation() })
    *
    *   // Future already running - just wraps existing Future
    *   val alreadyRunning = Future { expensiveComputation() }
    *   Lazy.fromFuture(alreadyRunning)
    * }}}
    */
  def fromFuture[A](f: => Future[A]): Lazy[A] =
    Lazy.FromFuture(f)

  /** Wraps resource acquisition/usage of scala.util.Using into a Lazy. */
  def using[R: Releasable, A](resource: => R)(f: R => A): Lazy[A] =
    Lazy.fromTry {
      scala.util.Using(resource) { r =>
        f(r)
      }
    }

  /** Wraps resource acquisition/usage of scala.util.Using.Manager into a Lazy.
    * The inner code should not be lazy, or async, as the manager will be closed
    * before resources can be used.
    */
  def usingManager[A](f: Using.Manager => A): Lazy[A] = {
    Lazy.fromTry {
      Using.Manager { manager =>
        f(manager)
      }
    }
  }

  /** Lazily creates a resource, using the provided manager. This method is
    * useful when used inside Lazy.usingManager { implicit manager => ... }. All
    * code inside Lazy.usingManager should be synchronous, so if building a
    * Lazy, it should be evaluated inside before the manager closes the
    * resources.
    */
  def managed[R: Releasable](resource: => R)(using
      manager: Using.Manager
  ): Lazy[R] =
    Lazy.fn(manager(resource))

  /** Evaluates the lzy function when the condition is true, mapping the result
    * to Some, or None if the condition is false
    */
  def when[A](cond: => Boolean)(lzy: => Lazy[A]): Lazy[Option[A]] = {
    Lazy.fn {
      if cond then lzy.map(Some(_))
      else Lazy.unit.as(None)
    }
  }.flatten

  /** Evaluates the partial function using [[when]] if defined at the provided
    * value a.
    */
  def whenCase[A, B](
      a: A
  )(pf: PartialFunction[A, Lazy[B]]): Lazy[Option[B]] = {
    when(pf.isDefinedAt(a))(pf(a))
  }

  /** Lift an Option into a Lazy, failing with NoSuchElementException if the
    * Option is empty.
    */
  def fromOption[A](opt: => Option[A]): Lazy[A] =
    Lazy.fn {
      opt match {
        case Some(a) => Lazy.fn(a)
        case None    =>
          Lazy.fail(new NoSuchElementException("None.get in Lazy.fromOption"))
      }
    }.flatten

}
