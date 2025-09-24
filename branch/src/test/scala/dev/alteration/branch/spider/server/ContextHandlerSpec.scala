package dev.alteration.branch.spider.server

import com.sun.net.httpserver.{Authenticator, Filter}
import dev.alteration.branch.spider.HttpMethod

class ContextHandlerSpec extends munit.FunSuite {

  test("semigroup - combine ContextHandlers") {
    val ctxA = new ContextHandler("/") {
      override val filters: Seq[Filter]                 = Seq.empty
      override val authenticator: Option[Authenticator] = Option.empty
      override val contextRouter
          : PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
        case HttpMethod.GET -> ("a" :: Nil) => RequestHandler.unimplementedHandler
      }
    }

    val ctxB = new ContextHandler("/b") {
      override val filters: Seq[Filter]                 = Seq(ContextHandler.timingFilter)
      override val authenticator: Option[Authenticator] = Option.empty
      override val contextRouter
          : PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
        case HttpMethod.GET -> ("b" :: Nil) => RequestHandler.unimplementedHandler
      }
    }

    val ctxC = ctxA |+| ctxB

    assert(ctxC.path == "/")
    assert(ctxC.contextRouter.isDefinedAt((HttpMethod.GET, List("a"))))
    assert(ctxC.contextRouter.isDefinedAt((HttpMethod.GET, List("b"))))
    assert(ctxC.filters == Seq(ContextHandler.timingFilter))
  }

}
