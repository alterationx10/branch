package dev.wishingtree.branch.spider.server

import com.sun.net.httpserver.{Authenticator, Filter}
import dev.wishingtree.branch.spider.HttpMethod
import dev.wishingtree.branch.spider.server.OpaqueSegments.*

class ContextHandlerSpec extends munit.FunSuite {

  test("semigroup - combine ContextHandlers") {
    val ctxA = new ContextHandler("/") {
      override val filters: Seq[Filter]                 = Seq.empty
      override val authenticator: Option[Authenticator] = Option.empty
      override val contextRouter
          : PartialFunction[(HttpMethod, Segments), RequestHandler[?, ?]] = {
        case HttpMethod.GET -> >> / "a" => RequestHandler.unimplementedHandler
      }
    }

    val ctxB = new ContextHandler("/b") {
      override val filters: Seq[Filter]                 = Seq(ContextHandler.timingFilter)
      override val authenticator: Option[Authenticator] = Option.empty
      override val contextRouter
          : PartialFunction[(HttpMethod, Segments), RequestHandler[?, ?]] = {
        case HttpMethod.GET -> >> / "b" => RequestHandler.unimplementedHandler
      }
    }

    val ctxC = ctxA |+| ctxB

    assert(ctxC.path == "/")
    assert(ctxC.contextRouter.isDefinedAt((HttpMethod.GET, >> / "a")))
    assert(ctxC.contextRouter.isDefinedAt((HttpMethod.GET, >> / "b")))
    assert(ctxC.filters == Seq(ContextHandler.timingFilter))
  }

}
