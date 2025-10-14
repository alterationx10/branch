package dev.alteration.branch.piggy

import dev.alteration.branch.macaroni.poolers.ResourcePool
import java.sql.{Connection, PreparedStatement, ResultSet}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.*

sealed trait Sql[+A] {

  /** FlatMap the result of this Sql operation with the given function.
    */
  final def flatMap[B](f: A => Sql[B]): Sql[B] =
    Sql.FlatMap(this, f)

  /** Map the result of this Sql operation with the given function.
    */
  final def map[B](f: A => B): Sql[B] =
    this.flatMap(a => Sql.MappedValue(f(a)))

  /** Flatten the result of this Sql operation.
    */
  final def flatten[B](using ev: A <:< Sql[B]): Sql[B] =
    this.flatMap(a => ev(a))

  /** Attempt to recover from a failure in this Sql operation by executing the
    * given function.
    */
  final def recover[B >: A](f: Throwable => Sql[B]): Sql[B] =
    Sql.Recover(this, f)

}

object Sql {

  /** A Helper type equivalent to X => PsArgHolder */
  type PsArg[X] = X => PsArgHolder

  /** A type-erased wrapper for a prepared statement argument that maintains
    * type safety through the PreparedStatementSetter type class.
    */
  trait PreparedStatementArg {
    def set(ps: PreparedStatement, index: Int): Unit
  }

  object PreparedStatementArg {

    /** Implicit conversion from any type A to PreparedStatementArg, provided
      * there is a PreparedStatementSetter[A] in scope.
      */
    given [A](using
        setter: PreparedStatementSetter[A]
    ): Conversion[A, PreparedStatementArg] with {
      def apply(value: A): PreparedStatementArg = new PreparedStatementArg {
        def set(ps: PreparedStatement, index: Int): Unit =
          setter.set(ps)(index)(value)
      }
    }
  }

  extension (sc: StringContext) {

    /** A string interpolator for creating prepared statements by capturing
      * arguments to aPsArgHolder.
      */
    def ps(args: PreparedStatementArg*): PsArgHolder = PsArgHolder(
      sc.s(args.map(_ => "?")*),
      args*
    )

    /** A string interpolator for creating UPDATE/INSERT/DELETE statements that
      * returns the number of affected rows. For single-use statements.
      * @example
      *   {{{
      *   psUpdate"INSERT INTO users (name, email) VALUES ($name, $email)"
      *   }}}
      */
    def psUpdate(args: PreparedStatementArg*): Sql[Int] = {
      val holder = PsArgHolder(sc.s(args.map(_ => "?")*), args*)
      Sql.PreparedUpdate(_ => holder, Seq(()))
    }

    /** A string interpolator for creating SQL with named parameters. Returns a
      * builder that requires calling .bindUpdate() or .bindQuery() to provide
      * parameter values. Do not use ${} interpolation - use :paramName
      * placeholders instead.
      * @example
      *   {{{
      *   psNamed"SELECT * FROM users WHERE name = :name AND age > :age"
      *     .bindQuery[User](
      *       "name" -> "John",
      *       "age" -> 25
      *     )
      *   }}}
      */
    def psNamed(args: Any*): NamedSqlBuilder = {
      if (args.nonEmpty) {
        throw new IllegalArgumentException(
          "psNamed does not support ${} interpolation. Use :paramName placeholders and .bindUpdate()/.bindQuery() instead."
        )
      }
      val sql = sc.parts.mkString
      new NamedSqlBuilder(sql)
    }
  }

  /** Builder for SQL statements with named parameters. Use .bindUpdate() for
    * INSERT/UPDATE/DELETE or .bindQuery() for SELECT statements.
    */
  class NamedSqlBuilder(sqlTemplate: String) {

    /** Bind named parameters for an UPDATE/INSERT/DELETE query.
      * @param params
      *   Tuple of (paramName, value) pairs
      * @return
      *   Sql[Int] representing the number of affected rows
      * @example
      *   {{{
      *   psNamed"INSERT INTO users (name, age) VALUES (:name, :age)"
      *     .bindUpdate(
      *       "name" -> "John",
      *       "age" -> 30
      *     )
      *   }}}
      */
    def bindUpdate(params: (String, PreparedStatementArg)*): Sql[Int] = {
      val namedParams = params.toMap
      val holder      = NamedParameterParser.toPsArgHolder(sqlTemplate, namedParams)
      Sql.PreparedUpdate(_ => holder, Seq(()))
    }

    /** Bind named parameters for a SELECT query.
      * @param params
      *   Tuple of (paramName, value) pairs
      * @return
      *   Sql[Seq[R]] representing the query results
      * @example
      *   {{{
      *   psNamed"SELECT * FROM users WHERE name = :name"
      *     .bindQuery[User]("name" -> "John")
      *   }}}
      */
    inline def bindQuery[R](params: (String, PreparedStatementArg)*)(using
        parser: ResultSetParser[R]
    ): Sql[Seq[R]] = {
      val namedParams = params.toMap
      val holder      = NamedParameterParser.toPsArgHolder(sqlTemplate, namedParams)
      Sql.PreparedQuery[Any, Unit, R](
        _ => holder,
        rs => rs.parsedList[R],
        Seq(())
      )
    }
  }

  extension [A](a: Sql[A]) {

    /** Execute this Sql operation using the given Connection. See
      * [[SqlRuntime.execute]].
      */
    def execute(using
        connection: Connection
    ): Try[A] =
      SqlRuntime.execute(a)

    /** Execute this Sql operation using the given Connection, returning the
      * result as a Future. See [[SqlRuntime.executeAsync]].
      */
    def executeAsync(using
        connection: Connection,
        executionContext: ExecutionContext
    ): Future[A] =
      SqlRuntime.executeAsync(a)

    /** Execute this Sql operation using the given ResourcePool[Connection]. See
      * [[SqlRuntime.executePool]].
      */
    def executePool(using
        pool: ResourcePool[Connection]
    ): Try[A] =
      SqlRuntime.executePool(a)

    /** Execute this Sql operation using the given ResourcePool[Connection],
      * returning the result as a Future. See [[SqlRuntime.executePoolAsync]].
      */
    def executePoolAsync(using
        pool: ResourcePool[Connection],
        executionContext: ExecutionContext
    ): Future[A] =
      SqlRuntime.executePoolAsync(a)
  }

  extension (rs: ResultSet) {

    /** Parse the ResultSet into a List of Tuples.
      */
    inline def parsedList[A](using parser: ResultSetParser[A]): List[A] = {
      val b = List.newBuilder[A]
      while rs.next() do b += parser.parse(rs)
      b.result()
    }

    /** Parse the ResultSet into an Option of a Tuple.
      */
    inline def parsed[A](using parser: ResultSetParser[A]): Option[A] = {
      parsedList[A].headOption
    }

  }

  private[piggy] final case class StatementRs[A](
      sql: String,
      fn: ResultSet => A
  ) extends Sql[A]

  private[piggy] final case class StatementCount(
      sql: String
  ) extends Sql[Int]

  /** A holder for a prepared statement string and its arguments.
    */
  final case class PsArgHolder(
      psStr: String,
      psArgs: PreparedStatementArg*
  ) {

    /** Set the arguments on the given PreparedStatement.
      */
    def set(preparedStatement: PreparedStatement): Unit = {
      psArgs.zipWithIndex.foreach { case (arg, i) =>
        arg.set(preparedStatement, i + 1)
      }
    }

    /** Set the arguments on the given PreparedStatement and execute it.
      */
    def setAndExecute(preparedStatement: PreparedStatement): Unit = {
      this.set(preparedStatement)
      preparedStatement.execute()
    }

    /** Set the arguments on the given PreparedStatement and executeUpdate it.
      */
    def setAndExecuteUpdate(preparedStatement: PreparedStatement): Int = {
      this.set(preparedStatement)
      preparedStatement.executeUpdate()
    }

    /** Set the arguments on the given PreparedStatement and executeQuery it.
      */
    inline def setAndExecuteQuery(
        preparedStatement: PreparedStatement
    ): ResultSet = {
      this.set(preparedStatement)
      preparedStatement.executeQuery()
    }

  }

  private[piggy] final case class PreparedExec[A, P](
      sqlFn: P => PsArgHolder,
      args: Seq[P]
  ) extends Sql[Unit]

  private[piggy] final case class PreparedUpdate[A, P](
      sqlFn: P => PsArgHolder,
      args: Seq[P]
  ) extends Sql[Int]

  private[piggy] final case class PreparedQuery[A, P, R](
      sqlFn: P => PsArgHolder,
      rsFn: ResultSet => Seq[R],
      args: Seq[P]
  ) extends Sql[Seq[R]]

  private[piggy] final case class FlatMap[A, B](
      sql: Sql[A],
      fn: A => Sql[B]
  ) extends Sql[B]

  private[piggy] final case class Recover[A](
      sql: Sql[A],
      fm: Throwable => Sql[A]
  ) extends Sql[A]

  private[piggy] final case class MappedValue[A](a: A) extends Sql[A]

  private[piggy] final case class Fail[A](e: Throwable) extends Sql[A]

  /** Create a Sql operation from a statement and a function to parse the
    * ResultSet.
    */
  def statement[A](sql: String, fn: ResultSet => A): Sql[A] =
    Sql.StatementRs(sql, fn)

  /** Create a Sql operation from a statement that's expected to return the
    * number of rows affected.
    */
  def statement(sql: String): Sql[Int] =
    Sql.StatementCount(sql)

  /** Create a Sql operation from a prepared statement function and a sequence
    * of arguments.
    */
  def prepare[I](q: I => PsArgHolder, args: I*): Sql[Unit] =
    Sql.PreparedExec(q, args.toSeq)

  /** Create a Sql operation from a prepared statement function and a sequence
    * of arguments, returning the number of rows affected.
    */
  def prepareUpdate[I](q: I => PsArgHolder, args: I*): Sql[Int] =
    Sql.PreparedUpdate(q, args.toSeq)

  /** Create a Sql operation from a prepared statement function and a sequence
    * of arguments, returning a sequence of Tuples.
    */
  inline def prepareQuery[I, R](
      q: I => PsArgHolder,
      args: I*
  )(using parser: ResultSetParser[R]): Sql[Seq[R]] =
    Sql.PreparedQuery(q, rs => rs.parsedList[R], args.toSeq)

  /** Create a Sql that always fails with the given Throwable.
    */
  def fail[A](throwable: Throwable): Sql[A] = Sql.Fail(throwable)
}
