package dev.wishingtree.branch.piggy

import dev.wishingtree.branch.macaroni.poolers.ResourcePool

import java.sql.{Connection, PreparedStatement}
import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.*

private[piggy] trait SqlRuntime {

  def execute[A](
      sql: Sql[A],
      d: Duration
  )(using connection: Connection, executionContext: ExecutionContext): Try[A]

  def executePool[A, B <: ResourcePool[Connection]](
      sql: Sql[A],
      d: Duration
  )(using
      pool: B,
      executionContext: ExecutionContext
  ): Try[A]

  def executeAsync[A](
      sql: Sql[A]
  )(using connection: Connection, executionContext: ExecutionContext): Future[A]

  def executePoolAsync[A, B <: ResourcePool[Connection]](sql: Sql[A])(using
      pool: B,
      executionContext: ExecutionContext
  ): Future[A]
}

object SqlRuntime extends SqlRuntime {

  /** Execute a Sql[A] using a Connection, returning the result as a Try. The
    * entire chain of Sql operations is done over the given Connection, and the
    * transaction is rolled back on failure.
    */
  override def execute[A](sql: Sql[A], d: Duration = Duration.Inf)(using
      connection: Connection,
      executionContext: ExecutionContext
  ): Try[A] =
    Try {
      try {
        connection.setAutoCommit(false)
        val result = Await.result(evalF(sql), d)
        connection.commit()
        result
      } catch {
        case e: Throwable =>
          connection.rollback()
          throw e
      } finally {
        connection.setAutoCommit(true)
      }

    }

  /** Execute a Sql[A] using a ResourcePool[Connection], returning the result as
    * a Try. The entire chain of Sql operations is done over a single Connection
    * from the pool, and the transaction is rolled back on failure.
    */
  override def executePool[A, B <: ResourcePool[Connection]](
      sql: Sql[A],
      d: Duration = Duration.Inf
  )(using
      pool: B,
      executionContext: ExecutionContext
  ): Try[A] =
    Try {
      pool.use { conn =>
        execute(sql)(using conn)
      }
    }.flatten

  /** Execute a Sql[A] using a Connection, returning the result as a Future. The
    * entire chain of Sql operations is done over the given Connection, and the
    * transaction is rolled back on failure.
    */
  override def executeAsync[A](sql: Sql[A])(using
      connection: Connection,
      executionContext: ExecutionContext
  ): Future[A] = Future(Future.fromTry(execute(sql))).flatten

  /** Execute a Sql[A] using a ResourcePool[Connection], returning the result as
    * a Future. The entire chain of Sql operations is done over a single
    * Connection from the pool, and the transaction is rolled back on failure.
    */
  override def executePoolAsync[A, B <: ResourcePool[Connection]](sql: Sql[A])(
      using
      pool: B,
      executionContext: ExecutionContext
  ): Future[A] = Future {
    pool.use(conn => executeAsync(sql)(using conn))
  }.flatten

  @tailrec
  private final def evalF[A](sql: Sql[A])(using
      connection: Connection,
      executionContext: ExecutionContext
  ): Future[A] = {
    sql match {
      case Sql.StatementRs(sql, fn)             =>
        Future.fromTry {
          Using.Manager { use =>
            val statement = use(connection.createStatement())
            val res       = statement.execute(sql)
            val rs        = use(statement.getResultSet)
            fn(rs)
          }
        }
      case Sql.StatementCount(sql)              =>
        Future.fromTry {
          Using.Manager { use =>
            val statement = use(connection.createStatement())
            val res       = statement.execute(sql)
            statement.getUpdateCount
          }
        }
      case Sql.PreparedExec(sqlFn, args)        =>
        Future.fromTry {
          Using.Manager { use =>
            val helpers               = args.map(sqlFn)
            val ps: PreparedStatement =
              use(connection.prepareStatement(helpers.head.psStr))
            helpers.foreach(_.setAndExecute(ps))
          }
        }
      case Sql.PreparedUpdate(sqlFn, args)      =>
        Future.fromTry {
          Using.Manager { use =>
            val helpers               = args.map(sqlFn)
            val ps: PreparedStatement =
              use(connection.prepareStatement(helpers.head.psStr))
            val counts: Seq[Int]      = helpers.map(_.setAndExecuteUpdate(ps))
            counts.foldLeft(0)(_ + _)
          }
        }
      case Sql.PreparedQuery(sqlFn, rsFn, args) =>
        Future.fromTry {
          Using.Manager { use =>
            val helpers               = args.map(sqlFn)
            val ps: PreparedStatement =
              use(connection.prepareStatement(helpers.head.psStr))
            helpers.flatMap { h =>
              rsFn(h.setAndExecuteQuery(ps))
            }
          }
        }
      case Sql.Fail(e)                          =>
        Future.failed(e)
      case Sql.MappedValue(a)                   =>
        Future.successful(a)
      case Sql.Recover(sql, fm)                 =>
        evalRecover(sql, fm)
      case Sql.FlatMap(sql, fn)                 =>
        sql match {
          case Sql.FlatMap(s, f)  =>
            evalF(s.flatMap(f(_).flatMap(fn)))
          case Sql.Recover(s, f)  =>
            evalRecoverFlatMap(s, f, fn)
          case Sql.MappedValue(a) =>
            evalF(fn(a))
          case s                  =>
            evalFlatMap(s, fn)
        }
    }
  }

  private def evalRecoverFlatMap[A, B](
      sql: Sql[A],
      rf: Throwable => Sql[A],
      fm: A => Sql[B]
  )(using Connection, ExecutionContext): Future[B] = {
    evalF(sql)
      .recoverWith { case t: Throwable => evalF(rf(t)) }
      .flatMap(a => evalF(fm(a)))
  }

  private def evalFlatMap[A, B](
      sql: Sql[A],
      fn: A => Sql[B]
  )(using Connection, ExecutionContext): Future[B] = {
    evalF(sql).flatMap(r => evalF(fn(r)))
  }

  private def evalRecover[A](
      sql: Sql[A],
      f: Throwable => Sql[A]
  )(using Connection, ExecutionContext): Future[A] = {
    evalF(sql).recoverWith { case t: Throwable => evalF(f(t)) }
  }

}
