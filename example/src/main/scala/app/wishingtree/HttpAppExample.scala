package app.wishingtree

import com.sun.net.httpserver.Filter
import dev.wishingtree.branch.spider.Paths.*
import dev.wishingtree.branch.spider.{
  ContextHandler,
  HttpApp,
  HttpVerb,
  RequestHandler
}

object HttpAppExample extends HttpApp {

  import RequestHandler.given

  case class SubGetter() extends RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response(Map.empty, "Aloha")
    }
  }

  val myhandler = new ContextHandler("/") {
    override val filters: Seq[Filter] = Seq(
      ContextHandler.timingFilter
    )
    override val contextRouter
        : PartialFunction[(HttpVerb, Path), RequestHandler[?, ?]] = {
      case HttpVerb.GET -> >> / "some" / "path" / s"$arg" => SubGetter()
    }
  }

  ContextHandler.registerHandler(myhandler)
}
