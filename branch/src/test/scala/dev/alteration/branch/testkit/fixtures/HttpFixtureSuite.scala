package dev.alteration.branch.testkit.fixtures

import com.sun.net.httpserver.HttpServer
import dev.alteration.branch.macaroni.runtimes.BranchExecutors
import dev.alteration.branch.spider.HttpMethod
import dev.alteration.branch.spider.server.{ContextHandler, RequestHandler}
import munit.FunSuite

import java.net.InetSocketAddress

trait HttpFixtureSuite extends FunSuite {

  type PF = PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]]

  def httpFixture(routes: PF): FunFixture[HttpServer] =
    FunFixture[HttpServer](
      setup = { _ =>
        val port: Int = scala.util.Random.between(10000, 11000)
        val server    = HttpServer.create(new InetSocketAddress(port), 0)
        server.setExecutor(BranchExecutors.executorService)
        server.start()
        ContextHandler.registerHandler(new ContextHandler("/") {
          override val contextRouter: PF = routes
        })(using server)
        server
      },
      teardown = { server =>
        server.stop(0)
      }
    )

}
