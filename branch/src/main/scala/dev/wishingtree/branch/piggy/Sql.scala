package dev.wishingtree.branch.piggy

import dev.wishingtree.branch.macaroni.poolers.ResourcePool

import java.sql.{Connection, PreparedStatement, ResultSet}
import scala.compiletime.*
import scala.concurrent.Future
import scala.util.*

sealed trait Sql[+A] {

  final def flatMap[B](f: A => Sql[B]): Sql[B] =
    Sql.FlatMap(this, f)

  final def map[B](f: A => B): Sql[B] =
    this.flatMap(a => Sql.MappedValue(f(a)))

  final def flatten[B](using ev: A <:< Sql[B]) =
    this.flatMap(a => ev(a))

  final def recover[B >: A](f: Throwable => Sql[B]): Sql[B] =
    Sql.Recover(this, f)

}

object Sql {

  extension [T](t: T) {
    def tuple1: Tuple1[T] = Tuple1(t)
  }

  type PsArg[X] = X => PsArgHolder

  extension (sc: StringContext) {
    def ps(args: Any*): PsArgHolder = PsArgHolder(
      sc.s(args.map(_ => "?")*),
      args*
    )
  }

  extension [A](a: Sql[A]) {
    def execute(using pool: ResourcePool[Connection]): Try[A]         =
      SqlRuntime.execute(a)
    def executeAsync(using pool: ResourcePool[Connection]): Future[A] =
      SqlRuntime.executeAsync(a)
  }

  private inline def parseRs[T <: Tuple](rs: ResultSet)(index: Int): Tuple =
    inline erasedValue[T] match {
      case _: EmptyTuple =>
        EmptyTuple
      case _: (t *: ts)  =>
        summonInline[ResultSetGetter[t]].get(rs)(index) *: parseRs[ts](rs)(
          index + 1
        )
    }

  private inline def setPs[A](ps: PreparedStatement)(index: Int)(
      value: A
  ): Unit =
    summonInline[PreparedStatementSetter[A]].set(ps)(index)(value)

  extension (rs: ResultSet) {

    inline def tupled[A <: Tuple]: A = {
      rs.next() // dangerous, should be handled
      parseRs[A](rs)(1).asInstanceOf[A]
    }

    inline def tupledList[A <: Tuple]: List[A] = {
      val b = List.newBuilder[A]
      while rs.next() do b += parseRs[A](rs)(1).asInstanceOf[A]
      b.result()
    }

  }

  private[piggy] final case class StatementRs[A](
      sql: String,
      fn: ResultSet => A
  ) extends Sql[A]

  private[piggy] final case class StatementCount(
      sql: String
  ) extends Sql[Int]

  final case class PsArgHolder(
      psStr: String,
      psArgs: Any*
  ) {

    private def set(preparedStatement: PreparedStatement): Unit = {
      psArgs.zipWithIndex.map({ case (a, i) => a -> (i + 1) }).foreach {
        case (a: Int, i)            => preparedStatement.setInt(i, a)
        case (a: Long, i)           => preparedStatement.setLong(i, a)
        case (a: Float, i)          => preparedStatement.setFloat(i, a)
        case (a: Double, i)         => preparedStatement.setDouble(i, a)
        case (a: String, i)         => preparedStatement.setString(i, a)
        case (a: Tuple1[String], i) => preparedStatement.setString(i, a._1)
        case (a: Boolean, i)        => preparedStatement.setBoolean(i, a)
        case (u, i)                 =>
          throw new IllegalArgumentException(s"Unsupported type $u")
      }
    }

    def setAndExecute(preparedStatement: PreparedStatement): Unit = {
      this.set(preparedStatement)
      preparedStatement.execute()
    }

    def setAndExecuteUpdate(preparedStatement: PreparedStatement): Int = {
      this.set(preparedStatement)
      preparedStatement.executeUpdate()
    }

    inline def setAndExecuteQuery(
        preparedStatement: PreparedStatement
    ): ResultSet = {
      this.set(preparedStatement)
      preparedStatement.executeQuery()
    }

  }

  private[piggy] final case class PreparedExec[A, P <: Product](
      sqlFn: P => PsArgHolder,
      args: Seq[P]
  ) extends Sql[Unit]

  private[piggy] final case class PreparedUpdate[A, P <: Product](
      sqlFn: P => PsArgHolder,
      args: Seq[P]
  ) extends Sql[Int]

  private[piggy] final case class PreparedQuery[A, P, R <: Tuple](
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

  def statement[A](sql: String, fn: ResultSet => A): Sql[A] =
    Sql.StatementRs(sql, fn)

  def statement(sql: String): Sql[Int] =
    Sql.StatementCount(sql)

  def prepare[I <: Product](q: I => PsArgHolder, args: I*): Sql[Unit] =
    Sql.PreparedExec(q, args.toSeq)

  def prepareUpdate[I <: Product](q: I => PsArgHolder, args: I*): Sql[Int] =
    Sql.PreparedUpdate(q, args.toSeq)

  inline def prepareQuery[I <: Product, R <: Tuple](
      q: I => PsArgHolder,
      args: I*
  ): Sql[Seq[R]] =
    Sql.PreparedQuery(q, rs => rs.tupledList[R], args.toSeq)

}
