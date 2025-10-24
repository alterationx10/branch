package dev.alteration.branch.testkit.fixtures

import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.server.{RequestHandler, SpiderServer}
import munit.FunSuite

import scala.concurrent.{ExecutionContext, Future}

trait HttpFixtureSuite extends FunSuite {

  type PF = PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]]

  case class ServerFixture(server: SpiderServer, port: Int)

  def httpFixture(routes: PF): FunFixture[ServerFixture] =
    FunFixture[ServerFixture](
      setup = { _ =>
        val port   = scala.util.Random.between(10000, 11000)
        val server = new SpiderServer(port = port, router = routes)

        // Start server in background
        Future {
          server.start()
        }(ExecutionContext.global)

        // Give server time to start
        Thread.sleep(50)

        ServerFixture(server, port)
      },
      teardown = { fixture =>
        fixture.server.stop()
      }
    )

}
