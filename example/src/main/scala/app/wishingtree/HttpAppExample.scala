package app.wishingtree

import com.sun.net.httpserver.Filter
import dev.wishingtree.branch.spider.Paths.*
import dev.wishingtree.branch.spider.*

object HttpAppExample extends HttpApp {

  import RequestHandler.given
  case class GreeterGetter() extends RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response("Aloha")
    }
  }

  val alohaGreeter = GreeterGetter()

  case class EchoGetter(msg: String) extends RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response(msg)
        .textContent
        .withHeader("this" -> "that")
    }
  }

  val myhandler = new ContextHandler("/") {

    override val filters: Seq[Filter] = Seq(
      ContextHandler.timingFilter
    )

    override val contextRouter
        : PartialFunction[(HttpVerb, Path), RequestHandler[?, ?]] = {
      case HttpVerb.GET -> >> / "some" / "path"           => alohaGreeter
      case HttpVerb.GET -> >> / "some" / "path" / s"$arg" => EchoGetter(arg)
    }

  }

  ContextHandler.registerHandler(myhandler)
}
