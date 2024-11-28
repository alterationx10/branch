//> using target.scope test
package dev.wishingtree.branch.testkit.fixtures

import com.sun.net.httpserver.HttpServer
import dev.wishingtree.branch.lzy.LazyRuntime
import dev.wishingtree.branch.spider.HttpVerb
import dev.wishingtree.branch.spider.server.OpaqueSegments.*
import dev.wishingtree.branch.spider.server.{ContextHandler, RequestHandler}
import munit.FunSuite

import java.net.InetSocketAddress

trait HttpFixtureSuite extends FunSuite {

  type PF = PartialFunction[(HttpVerb, Segments), RequestHandler[?, ?]]

  def httpFixture(routes: PF): FunFixture[HttpServer] =
    FunFixture[HttpServer](
      setup = { test =>
        val port: Int = scala.util.Random.between(10000, 11000)
        val server    = HttpServer.create(new InetSocketAddress(port), 0)
        server.setExecutor(LazyRuntime.executorService)
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
