package dev.wishingtree.branch.piggy

import java.sql.{Connection, PreparedStatement, ResultSet}
import scala.compiletime.*
import scala.util.*

sealed trait Sql[+A] {}

object Sql {

  extension (sc: StringContext) {
    def ps(args: Any*): PsHelper = PsHelper(
      sc.s(args.map(_ => "?")*),
      args*
    )
  }

  extension [A](a: Sql[A]) {
    def execute(using pool: ResourcePool[Connection]): Try[A] =
      SqlRuntime.execute(a)
  }

  private inline def parseRs[T <: Tuple](rs: ResultSet)(index: Int): Tuple =
    inline erasedValue[T] match
      case _: EmptyTuple =>
        EmptyTuple
      case _: (t *: ts)  =>
        summonInline[ResultSetGetter[t]].get(rs)(index) *: parseRs[ts](rs)(
          index + 1
        )

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

  private[piggy] final case class StmntRs[A](
      sql: String,
      fn: ResultSet => A
  ) extends Sql[A]

  private[piggy] final case class StmntCount(
      sql: String
  ) extends Sql[Int]

  final case class PsHelper(
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

  private[piggy] final case class PrepExec[A, P <: Product](
      sqlFn: P => PsHelper,
      args: Seq[P]
  ) extends Sql[Unit]

  private[piggy] final case class PrepUpdate[A, P <: Product](
      sqlFn: P => PsHelper,
      args: Seq[P]
  ) extends Sql[Int]

  private[piggy] final case class PrepQuery[A, P, R <: Tuple](
      sqlFn: P => PsHelper,
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

  def statement[A](sql: String, fn: ResultSet => A): Sql[A] =
    Sql.StmntRs(sql, fn)

  def statement(sql: String): Sql[Int] =
    Sql.StmntCount(sql)

  def prepare[I <: Product](q: I => PsHelper, args: I*): Sql[Unit] =
    Sql.PrepExec(q, args.toSeq)

  def prepareUpdate[I <: Product](q: I => PsHelper, args: I*): Sql[Int] =
    Sql.PrepUpdate(q, args.toSeq)

  inline def prepareQuery[I <: Product, R <: Tuple](
      q: I => PsHelper,
      args: I*
  ): Sql[Seq[R]] =
    Sql.PrepQuery(q, rs => rs.tupledList[R], args.toSeq)

}
