package dev.wishingtree.branch.friday

import dev.wishingtree.branch.friday.Result.{Failure, Success}

import scala.util.matching.Regex

object Reference extends Parsers[Reference.Parser] {
  type Parser[+A] = Location => Result[A]

  def firstNonmatchingIndex(s1: String, s2: String, offset: Int): Int = {
    var i = 0
    while i + offset < s1.length && i < s2.length do {
      if s1.charAt(i + offset) != s2.charAt(i) then return i
      i += 1
    }
    if s1.length - offset >= s2.length then -1
    else s1.length - offset
  }

  override def string(s: String): Parser[String] = { l =>
    val i = firstNonmatchingIndex(l.input, s, l.offset)
    if i == -1 then 
      Success(s, s.length)
    else Failure(l.advanceBy(i).toError(s"'$s'"), i != 0)
  }

  override def succeed[A](a: A): Parser[A] =
    _ => Success(a, 0)

  override def fail(msg: String): Parser[Nothing] =
    l => Failure(l.toError(msg), true)

  override def regex(r: Regex): Parser[String] = { l =>
    r.findPrefixOf(l.remaining) match {
      case None    => Failure(l.toError(s"regex $r"), false)
      case Some(m) => Success(m, m.length)
    }
  }

  extension [A](p: Parser[A]) {
    override def run(input: String): Either[ParseError, A] =
      p(Location(input)).extract

    override def attempt: Parser[A] =
      l => p(l).uncommit

    override infix def or(p2: => Parser[A]): Parser[A] = { l =>
      p(l) match {
        case Failure(e, false) => p2(l)
        case r                 => r 
      }
    }

    override def flatMap[B](f: A => Parser[B]): Parser[B] = { l =>
      p(l) match {
        case Success(a, n)     =>
          f(a)(l.advanceBy(n))
            .addCommit(n != 0)
            .advanceSuccess(n)
        case f @ Failure(_, _) => f
      }
    }

    override def slice: Parser[String] = { l =>
      p(l) match {
        case Success(_, n)     => Success(l.slice(n), n)
        case f @ Failure(_, _) => f
      }
    }

    override def label(msg: String): Parser[A] =
      l => p(l).mapError(_.label(msg))

    override def scope(msg: String): Parser[A] =
      l => p(l).mapError(_.push(l, msg))

  }

}
