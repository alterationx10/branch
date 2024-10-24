package app.wishingtree

import com.sun.net.httpserver.Filter
import dev.wishingtree.branch.spider.{ContextHandler, HttpApp, RequestHandler}

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
    override val getHandler: RequestHandler[?, ?] = SubGetter()
  }

  ContextHandler.registerHandler(myhandler)
}
