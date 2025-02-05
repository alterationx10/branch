package dev.wishingtree.branch.ursula.args

import dev.wishingtree.branch.ursula.doc.{Documentation, FlagDoc}

import scala.annotation.tailrec
import dev.wishingtree.branch.ursula.extensions.Extensions.*
import dev.wishingtree.branch.veil.Veil

/** Flags are non-positional arguments passed to the command. Flags can be
  * generally used as either an argument flag, which expects an argument parsed
  * as type R, or boolean flags which do not.
  *
  * @tparam R
  *   The type expected to parse the flag argument as.
  */
trait Flag[R] {

  /** The name of the flag, e.g. "help". This will be parsed as s"--$name", e.g.
    * "--help"
    */
  val name: String

  /** A short-key version of name, e.g. "h". This will be parsed as
    * s"-$shortKey", e.g. "-h"
    */
  val shortKey: String

  /** A description of the purpose of this flag, used in [[documentation]].
    */
  val description: String

  /** Sets whether this Flag should be printed in help.
    */
  val hidden: Boolean = false

  /** Sets if this Flag can be used multiple times in one Command
    */
  val multiple: Boolean = false

  /** An optional environment variable used to set the value of the Flag when
    * [[expectsArgument]] is true. The precedence is [arg] > [[env]] >
    * [[default]]
    */
  val env: Option[String] = Option.empty

  private final def envArg: Option[R] =
    env.flatMap(e => Veil.get(e)).map(parse)

  /** An optional set of possible values to restrict the argument to. For
    * example, if you wanted to restrict an --env [arg] flag to only "dev", or
    * "test", supply them here.
    */
  val options: Option[Set[R]] = Option.empty

  /** A partial function that will take the String value of the passed argument,
    * and convert it to type R.
    *
    * Typical usage might be for parsing String to Int/Float/Custom Domain, e.g.
    * for a Flag[Int], parse = str => str.toInt.
    *
    * Advanced usage could be used to do more targeted work as well, such as
    * processing the argument directly, versus simply obtaining the value for it
    * to be parsed later, e.g. for a Flag[String], parse = str =>
    * str.toUpperCase
    * @return
    *   the evaluation of String => R
    */
  def parse: PartialFunction[String, R]

  /** Indicates this Flag expects an argument
    */
  val expectsArgument: Boolean = true

  /** An optional default value to provide
    */
  val default: Option[R] = Option.empty

  /** Indicates if this Flag is required or not.
    */
  val required: Boolean = false

  /** An optional set of Flags that need to also be present for this Flag to
    * function
    */
  val dependsOn: Option[Seq[Flag[?]]] = Option.empty

  /** An optional set of Flags that conflict with usage of this Flag
    */
  val exclusive: Option[Seq[Flag[?]]] = Option.empty

  final lazy val _sk: String = s"-$shortKey"
  final lazy val _lk: String = s"--$name"

  /** Checks if flags to trigger this Flag are present in the provided Command
    * arguments
    * @param args
    *   The arguments passed to the command
    * @return
    *   true if present, otherwise false
    */
  final def isPresent(args: Seq[String]): Boolean =
    args.find(a => a == _sk || a == _lk).isDefined

  private final def recursiveParse[A](
      fn: String => A
  )(args: Seq[String]): Seq[A] = {
    @tailrec
    def loop(a: Seq[String], r: Seq[A]): Seq[A] =
      a.toList match {
        case Nil                                   => r
        case _ :: Nil                              => r
        case f :: v :: _ if (f == _sk || f == _lk) =>
          loop(a.drop(2), r.appended(fn(v)))
        case _                                     =>
          loop(a.drop(1), r)
      }
    loop(args, Seq.empty)
  }

  /** A method that will find the first instance of a flag triggering this Flag,
    * if present, and evaluate the [[parse]] partial function on it.
    *
    * Most useful when this Flag is expected once (i.e. [[multiple]] == false)
    * @param args
    *   The arguments passed to the command
    * @throws IllegalArgumentException
    *   if the flag is present but the value is not in the options (if options
    *   is defined and non-empty)
    * @return
    *   An optional parsed value of type R
    * @see
    *   [[multiple]]
    */
  final def parseFirstArg(args: Seq[String]): Option[R] = {
    args
      .dropWhile(a => a != _sk && a != _lk)
      .drop(1)
      .headOption
      .map(parse) :~ envArg :~ default
  }.oneOfOrThrow(
    options.getOrElse(Set.empty),
    new IllegalArgumentException(s"Invalid option for flag $name")
  )

  /** Finds instances of arguments that trigger this Flag, and presses them
    * through [[parse]]
    * @param args
    *   The arguments passed to the command
    * @throws IllegalArgumentException
    *   if the flag is present but the value is not in the options (if options
    *   is defined and non-empty)
    * @return
    *   A sequence of parsed values of type R
    */
  def parseArgs(args: Seq[String]): Seq[R] = {
    recursiveParse(parse)(args) :~ envArg.toSeq :~ default.toSeq
  }.oneOfOrThrow(
    options.getOrElse(Set.empty),
    new IllegalArgumentException(s"Invalid option for flag $name")
  )

  lazy val documentation: Documentation = FlagDoc(this)

}

trait BooleanFlag extends Flag[Unit] {
  override def parse: PartialFunction[String, Unit] = _ => ()
  override val expectsArgument: Boolean             = false
}

trait StringFlag extends Flag[String] {
  override def parse: PartialFunction[String, String] = identity[String](_)
}

trait IntFlag extends Flag[Int] {
  override def parse: PartialFunction[String, Int] = str => str.toInt
}
