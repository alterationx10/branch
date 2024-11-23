package dev.wishingtree.branch.spider.server

import com.sun.net.httpserver.{HttpHandler, HttpServer}
import dev.wishingtree.branch.lzy.LazyRuntime

import java.net.InetSocketAddress

trait SpiderApp {

  val port: Int =
    9000

  val backlog: Int =
    0

  given server: HttpServer =
    HttpServer.create(new InetSocketAddress(port), backlog)

  server.setExecutor(LazyRuntime.executorService)

  Runtime.getRuntime.addShutdownHook {
    new Thread(() => server.stop(5))
  }

  final def main(args: Array[String]): Unit =
    server.start()

}
