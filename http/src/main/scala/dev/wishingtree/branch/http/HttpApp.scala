package dev.wishingtree.branch.http

import com.sun.net.httpserver.{HttpHandler, HttpServer}
import dev.wishingtree.branch.lzy.LazyRuntime

import java.net.InetSocketAddress

trait HttpApp {

  val server: HttpServer =
    HttpServer.create(new InetSocketAddress(9000), 0)

  server.setExecutor(LazyRuntime.executorService)
  def registerHandler(path: String)(handler: HttpHandler) =
    server
      .createContext(path)
      .setHandler(handler)
    

  final def main(args: Array[String]): Unit =
    server.start()

}
