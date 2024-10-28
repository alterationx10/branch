package dev.wishingtree.branch.piggy

import dev.wishingtree.branch.lzy.LazyRuntime

import java.sql.{Connection, PreparedStatement}
import java.util.concurrent.{CompletableFuture, ExecutorService}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Using}
import scala.jdk.FutureConverters.*

trait SqlRuntime {
  def execute[A, B <: ResourcePool[Connection]](sql: Sql[A])(using
      pool: B
  ): Try[A]

  def executeAsync[A, B <: ResourcePool[Connection]](sql: Sql[A])(using
      pool: B
  ): Future[A]
}

object SqlRuntime extends SqlRuntime {

  val executorService: ExecutorService =
    LazyRuntime.executorService

  val executionContext: ExecutionContext =
    LazyRuntime.executionContext

  override def execute[A, B <: ResourcePool[Connection]](sql: Sql[A])(using
      pool: B
  ): Try[A] =
    eval(sql)(using pool).get()

  override def executeAsync[A, B <: ResourcePool[Connection]](sql: Sql[A])(using
      pool: B
  ): Future[A] =
    eval(sql).asScala.flatMap(t => Future.fromTry(t))(executionContext)

  private def eval[A, B <: ResourcePool[Connection]](sql: Sql[A])(using
      pool: B
  ): CompletableFuture[Try[A]] = {
    CompletableFuture.supplyAsync(
      () => {
        sql match
          case Sql.StmntRs(sql, fn)              =>
            Try {
              pool.use { conn =>
                Using.Manager { use =>
                  val statement = use(conn.createStatement())
                  val res       = statement.execute(sql)
                  val rs        = use(statement.getResultSet)
                  fn(rs)
                }
              }
            }.flatten
          case Sql.StmntCount(sql)               =>
            Try {
              pool.use { conn =>
                Using.Manager { use =>
                  val statement = use(conn.createStatement())
                  val res       = statement.execute(sql)
                  statement.getUpdateCount
                }
              }
            }.flatten
          case Sql.PrepExec(sqlFn, args)         =>
            Try {
              pool.use { conn =>
                Using.Manager { use =>
                  val helpers               = args.map(sqlFn)
                  val ps: PreparedStatement =
                    use(conn.prepareStatement(helpers.head.psStr))
                  helpers.foreach(_.setAndExecute(ps))
                }
              }
            }.flatten
          case Sql.PrepUpdate(sqlFn, args)       =>
            Try {
              pool.use { conn =>
                Using.Manager { use =>
                  val helpers               = args.map(sqlFn)
                  val ps: PreparedStatement =
                    use(conn.prepareStatement(helpers.head.psStr))
                  helpers.map(_.setAndExecuteUpdate(ps)).sum
                }
              }
            }.flatten
          case Sql.PrepQuery(sqlFn, rsFun, args) =>
            Try {
              pool.use { conn =>
                Using.Manager { use =>
                  val helpers               = args.map(sqlFn)
                  val ps: PreparedStatement =
                    use(conn.prepareStatement(helpers.head.psStr))
                  helpers.flatMap { h =>
                    rsFun(h.setAndExecuteQuery(ps))
                  }
                }
              }
            }.flatten
      },
      executorService
    )
  }
}
