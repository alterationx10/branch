package dev.wishingtree.branch.testkit.fixtures

import com.sun.net.httpserver.HttpServer
import dev.wishingtree.branch.macaroni.runtimes.BranchExecutors
import dev.wishingtree.branch.spider.HttpMethod
import dev.wishingtree.branch.spider.server.{ContextHandler, RequestHandler}
import munit.FunSuite

import java.net.InetSocketAddress
import java.nio.file.Path

trait HttpFixtureSuite extends FunSuite {

  type PF = PartialFunction[(HttpMethod, Path), RequestHandler[?, ?]]

  def httpFixture(routes: PF): FunFixture[HttpServer] =
    FunFixture[HttpServer](
      setup = { test =>
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
