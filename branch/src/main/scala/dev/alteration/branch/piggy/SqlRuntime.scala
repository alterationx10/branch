package dev.alteration.branch.piggy

import dev.alteration.branch.macaroni.poolers.ResourcePool
import java.sql.{Connection, PreparedStatement}
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.*

private[piggy] trait SqlRuntime {

  def execute[A](
      sql: Sql[A]
  )(using connection: Connection): Try[A]

  def executePool[A, B <: ResourcePool[Connection]](
      sql: Sql[A]
  )(using pool: B): Try[A]

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
  override def execute[A](sql: Sql[A])(using
      connection: Connection
  ): Try[A] =
    Try {
      try {
        connection.setAutoCommit(false)
        val result = eval(sql).get
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
      sql: Sql[A]
  )(using pool: B): Try[A] =
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
  private final def eval[A](sql: Sql[A])(using
      connection: Connection
  ): Try[A] = {
    sql match {
      case Sql.StatementRs(sql, fn)             =>
        Using.Manager { use =>
          val statement = use(connection.createStatement())
          statement.execute(sql)
          val rs        = use(statement.getResultSet)
          fn(rs)
        }.recoverWith { case e: Throwable =>
          Failure(PiggyException.SqlExecutionException(sql, e))
        }
      case Sql.StatementCount(sql)              =>
        Using.Manager { use =>
          val statement = use(connection.createStatement())
          statement.execute(sql)
          statement.getUpdateCount
        }.recoverWith { case e: Throwable =>
          Failure(PiggyException.SqlExecutionException(sql, e))
        }
      case Sql.PreparedExec(sqlFn, args)        =>
        if (args.isEmpty) {
          Failure(PiggyException.EmptyArgumentException("PreparedExec"))
        } else {
          val helpers = args.map(sqlFn)
          val sql     = helpers.head.psStr
          Using.Manager { use =>
            val ps: PreparedStatement = use(connection.prepareStatement(sql))
            helpers.foreach { h =>
              h.set(ps)
              ps.addBatch()
            }
            ps.executeBatch()
            () // Return Unit
          }.recoverWith { case e: Throwable =>
            Failure(PiggyException.BatchExecutionException(sql, args.size, e))
          }
        }
      case Sql.PreparedUpdate(sqlFn, args)      =>
        if (args.isEmpty) {
          Failure(PiggyException.EmptyArgumentException("PreparedUpdate"))
        } else {
          val helpers = args.map(sqlFn)
          val sql     = helpers.head.psStr
          Using.Manager { use =>
            val ps: PreparedStatement = use(connection.prepareStatement(sql))
            helpers.foreach { h =>
              h.set(ps)
              ps.addBatch()
            }
            val counts: Array[Int]    = ps.executeBatch()
            counts.sum
          }.recoverWith { case e: Throwable =>
            Failure(PiggyException.BatchExecutionException(sql, args.size, e))
          }
        }
      case Sql.PreparedQuery(sqlFn, rsFn, args) =>
        if (args.isEmpty) {
          Failure(PiggyException.EmptyArgumentException("PreparedQuery"))
        } else {
          val helpers = args.map(sqlFn)
          val sql     = helpers.head.psStr
          Using.Manager { use =>
            val ps: PreparedStatement = use(connection.prepareStatement(sql))
            helpers.flatMap { h =>
              Using.resource(h.setAndExecuteQuery(ps)) { rs =>
                rsFn(rs)
              }
            }
          }.recoverWith { case e: Throwable =>
            Failure(
              PiggyException.PreparedStatementException(sql, helpers.size, e)
            )
          }
        }
      case Sql.Fail(e)                          =>
        Failure(e)
      case Sql.MappedValue(a)                   =>
        Success(a)
      case Sql.Recover(sql, fm)                 =>
        evalRecover(sql, fm)
      case Sql.FlatMap(sql, fn)                 =>
        sql match {
          case Sql.FlatMap(s, f)  =>
            eval(s.flatMap(f(_).flatMap(fn)))
          case Sql.Recover(s, f)  =>
            evalRecoverFlatMap(s, f, fn)
          case Sql.MappedValue(a) =>
            eval(fn(a))
          case s                  =>
            evalFlatMap(s, fn)
        }
    }
  }

  private def evalRecoverFlatMap[A, B](
      sql: Sql[A],
      rf: Throwable => Sql[A],
      fm: A => Sql[B]
  )(using Connection): Try[B] = {
    eval(sql)
      .recoverWith { case t: Throwable => eval(rf(t)) }
      .flatMap(a => eval(fm(a)))
  }

  private def evalFlatMap[A, B](
      sql: Sql[A],
      fn: A => Sql[B]
  )(using Connection): Try[B] = {
    eval(sql).flatMap(r => eval(fn(r)))
  }

  private def evalRecover[A](
      sql: Sql[A],
      f: Throwable => Sql[A]
  )(using Connection): Try[A] = {
    eval(sql).recoverWith { case t: Throwable => eval(f(t)) }
  }

}
