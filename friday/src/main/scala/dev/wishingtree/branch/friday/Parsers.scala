package dev.wishingtree.branch.friday

import java.util.regex.Pattern
import scala.annotation.targetName
import scala.language.postfixOps
import scala.util.matching.Regex

trait Parsers[Parser[+_]] {

  def string(s: String): Parser[String]

  def char(c: Char): Parser[Char] =
    string(c.toString).map(_.charAt(0))

  def defaultSucceed[A](a: A): Parser[A] =
    string("").map(_ => a)

  def succeed[A](a: A): Parser[A]

  def fail(msg: String): Parser[Nothing]

  def regex(r: Regex): Parser[String]

  def whitespace: Parser[String] = regex("\\s*".r)

  /** Parser which consumes 1 or more digits. */
  def digits: Parser[String] = regex("\\d+".r)

  /** Parser which consumes reluctantly until it encounters the given string. */
  def thru(s: String): Parser[String] = regex((".*?" + Pattern.quote(s)).r)

  /** Unescaped string literals, like "foo" or "bar". */
  def quoted: Parser[String] = string("\"") *> thru("\"").map(_.dropRight(1))

  /** Unescaped or escaped string literals, like "An \n important \"Quotation\""
    * or "bar".
    */
  def escapedQuoted: Parser[String] =
    // rather annoying to write, left as an exercise
    // we'll just use quoted (unescaped literals) for now
    quoted.label("string literal").token

  /** C/Java style floating point literals, e.g .1, -1.0, 1e9, 1E-23, etc.
    * Result is left as a string to keep full precision
    */
  def doubleString: Parser[String] =
    regex("[-+]?([0-9]*\\.)?[0-9]+([eE][-+]?[0-9]+)?".r).token

  /** Floating point literals, converted to a `Double`. */
  def double: Parser[Double] =
    doubleString.map(_.toDouble).label("double literal")

  /** A parser that succeeds when given empty input. */
  def eof: Parser[String] =
    regex("\\z".r).label("unexpected trailing characters")

  extension [A](p: Parser[A]) {

    def listOfN(n: Int): Parser[List[A]] =
      if n <= 0 then succeed(List.empty[A])
      else p.map2(p.listOfN(n - 1))(_ :: _)

    def many: Parser[List[A]] =
      p.map2(p.many)(_ :: _) | succeed(List.empty[A])

    def many1: Parser[List[A]] =
      p.map2(p.many)(_ :: _)

    def map[B](f: A => B): Parser[B] =
      p.flatMap(a => succeed(f(a)))

    def product[B](p2: => Parser[B]): Parser[(A, B)] =
      for {
        a <- p
        b <- p2
      } yield (a, b)

    @targetName("symbolicProduct")
    def **[B](p2: => Parser[B]): Parser[(A, B)] = p.product(p2)

    def map2[B, C](p2: => Parser[B])(f: (A, B) => C): Parser[C] =
      p.product(p2).map(f.tupled)

    def run(input: String): Either[ParseError, A]

    infix def or(p2: => Parser[A]): Parser[A]
    @targetName("symbolicOr")
    def |(p2: Parser[A]): Parser[A] = p.or(p2)

    def flatMap[B](f: A => Parser[B]): Parser[B]

    def slice: Parser[String]

    @targetName("keepRight")
    def *>[B](p2: => Parser[B]): Parser[B] =
      p.map2(p2)((_, b) => b)

    @targetName("keepLeft")
    def <*[B](p2: => Parser[B]): Parser[A] =
      p.map2(p2)((a, _) => a)

    def sep(separator: Parser[Any]): Parser[List[A]] =
      sep1(separator) | succeed(List.empty[A])

    def sep1(separator: Parser[Any]): Parser[List[A]] =
      p.map2((separator *> p).many)(_ :: _)

    def as[B](b: B): Parser[B] =
      p.map(_ => b)

    def label(msg: String): Parser[A]
    def scope(msg: String): Parser[A]

    def attempt: Parser[A]

    def token: Parser[A] = p.attempt <* whitespace

    def root: Parser[A] =
      p <* eof
  }

}
