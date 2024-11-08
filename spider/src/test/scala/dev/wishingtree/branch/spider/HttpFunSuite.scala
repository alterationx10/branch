package dev.wishingtree.branch.spider

import com.sun.net.httpserver.HttpServer
import dev.wishingtree.branch.lzy.LazyRuntime
import munit.FunSuite
import OpaqueSegments.*

import java.net.InetSocketAddress

trait HttpFunSuite extends FunSuite {

  type PF = PartialFunction[(HttpVerb, Segments), RequestHandler[?, ?]]

  val httpFixture = FunFixture[HttpServer](
    setup = { test =>
      val port: Int = scala.util.Random.between(10000, 11000)
      val server    = HttpServer.create(new InetSocketAddress(port), 0)
      server.setExecutor(LazyRuntime.executorService)
      server.start()
      server
    },
    teardown = { server =>
      server.stop(0)
    }
  )

  val contextFixture =
    FunFixture[(HttpServer, PF => ContextHandler)](
      setup = { test =>
        val port: Int = scala.util.Random.between(10000, 11000)
        val server    = HttpServer.create(new InetSocketAddress(port), 0)
        server.setExecutor(LazyRuntime.executorService)
        server.start()

        val genContextHandler: PF => ContextHandler = (pf: PF) =>
          new ContextHandler("/") {
            override val contextRouter
                : PartialFunction[(HttpVerb, Segments), RequestHandler[?, ?]] =
              pf
          }

        (server, genContextHandler)
      },
      teardown = { case (server, _) =>
        server.stop(0)
      }
    )

}
