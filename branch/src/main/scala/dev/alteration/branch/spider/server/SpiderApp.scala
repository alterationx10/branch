package dev.alteration.branch.spider.server

import com.sun.net.httpserver.HttpServer
import dev.alteration.branch.macaroni.runtimes.BranchExecutors

import java.net.InetSocketAddress

trait SpiderApp {

  /** The port on which the server will listen. Defaults to 9000.
    */
  val port: Int =
    9000

  /** The maximum number of pending connections the server will queue.
    */
  val backlog: Int =
    0

  /** The given server instance.
    */
  final given server: HttpServer =
    HttpServer.create(new InetSocketAddress(port), backlog)

  server.setExecutor(BranchExecutors.executorService)

  Runtime.getRuntime.addShutdownHook {
    new Thread(() => server.stop(5))
  }

  /** The application's main entry point, which starts the HTTP server.
    */
  final def main(args: Array[String]): Unit =
    server.start()

}
