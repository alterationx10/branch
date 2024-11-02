package dev.wishingtree.branch.macaroni.parsers

enum Result[+A] {
  case Success(get: A, length: Int)
  case Failure(get: ParseError, isCommitted: Boolean) extends Result[Nothing]

  def extract: Either[ParseError, A] = this match {
    case Failure(e, _) => Left(e)
    case Success(a, _) => Right(a)
  }
  
  def uncommit: Result[A] = this match {
    case Failure(e, true) => Failure(e, false)
    case _                => this
  }
  
  def addCommit(isCommitted: Boolean): Result[A] = this match {
    case Failure(e, c) => Failure(e, c || isCommitted)
    case _             => this
  }
  
  def mapError(f: ParseError => ParseError): Result[A] = this match {
    case Failure(e, c) => Failure(f(e), c)
    case _             => this
  }

  def advanceSuccess(n: Int): Result[A] = this match {
    case Success(a, m) => Success(a, n + m)
    case _             => this
  }
}
