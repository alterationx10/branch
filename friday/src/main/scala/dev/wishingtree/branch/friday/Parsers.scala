package dev.wishingtree.branch.friday

import dev.wishingtree.branch.friday.Json.*

import scala.annotation.targetName
import scala.language.postfixOps
import scala.util.matching.Regex

trait Parsers[Parser[+_]] {

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

    def run(input: String): Either[ParseError, A] = ???

    infix def or(p2: => Parser[A]): Parser[A] = ???
    @targetName("symbolicOr")
    def |(p2: Parser[A]): Parser[A]           = p.or(p2)

    def flatMap[B](f: A => Parser[B]): Parser[B] = ???

    def slice: Parser[String] = ???

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
    def scope(msg: String):  Parser[A]

  }

  // Extension above

  case class Location(input: String, offset: Int = 0) {
    lazy val line = input.slice(0, offset + 1).count(_ == '\n') + 1
    lazy val col  = input.slice(0, offset + 1).lastIndexOf('\n') match {
      case -1        => offset + 1
      case lineStart => offset - lineStart
    }
  }
  
  case class ParseError(stack: List[(Location, String)])

  def errorLocation(e: ParseError): Location
  def errorMessage(e: ParseError): String

  def succeed[A](a: A): Parser[A] =
    string("").map(_ => a)

  def char(c: Char): Parser[Char] =
    string(c.toString).map(_.charAt(0))

  def string(s: String): Parser[String] = ???

  def regex(r: Regex): Parser[String] = ???

  def eof: Parser[String] =
    regex("\\z".r) // .label("unexpected trailing characters")


  val value: Parser[Json] = ???

  def keyVal: Parser[(String, Json)] =
    escapedQuote ** (token(":") *> value)

  def obj: Parser[Json] =
    token("{") *> keyVal.sep(token(",")).map { kvs =>
      JsonObject(kvs.toMap)
    } <* token("}")

  def token(s: String): Parser[String] =
    string(s) <* whitespace

  def array: Parser[Json] =
    token("[") *>
      value.sep(token(",")).map(vs => JsonArray(vs.toIndexedSeq))
      <* token("]")

  def double: Parser[Double]       = ???
  def escapedQuote: Parser[String] = ???

  def literal: Parser[Json] = {
    token("null").as(JsonNull) |
      double.map(JsonNumber.apply) |
      escapedQuote.map(JsonString.apply) |
      token("true").as(JsonBool(true)) |
      token("false").as(JsonBool(false))
  }

  def whitespace: Parser[String] = regex("\\s*".r)
  def document: Parser[Json]     = whitespace *> (array | obj) <* eof

}
