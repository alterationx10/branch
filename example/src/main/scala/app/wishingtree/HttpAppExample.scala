package app.wishingtree

import dev.wishingtree.branch.http.{HttpApp, RequestHandler, RouteHandler}

object HttpAppExample extends HttpApp {

  case class SubGetter() extends RequestHandler[Unit, String] {

    override def requestDecoder: Conversion[Array[Byte], Unit] = _ => ()

    override def responseEncoder: Conversion[String, Array[Byte]] = _.getBytes()

    override def handle(request: Request[Unit]): Response[String] = {
      Response(Map.empty, "Aloha")
    }
  }

  val myhandler = new RouteHandler("/") {
    override lazy val getHandler: RequestHandler[_, _] = SubGetter()
  }

  RouteHandler.registerHandler(myhandler)
}
