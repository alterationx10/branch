package app.wishingtree

import dev.wishingtree.branch.spider.{HttpApp, RequestHandler, RouteHandler}

object HttpAppExample extends HttpApp {
  
  import RequestHandler.given 
  
  case class SubGetter() extends RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response(Map.empty, "Aloha")
    }
  }

  val myhandler = new RouteHandler("/") {
    override lazy val getHandler: RequestHandler[_, _] = SubGetter()
  }

  RouteHandler.registerHandler(myhandler)
}
