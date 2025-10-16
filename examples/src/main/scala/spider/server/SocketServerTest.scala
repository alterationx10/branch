package spider.server

import dev.alteration.branch.spider.server.SocketServer

/** A simple test application to verify SocketServer Phase 1 functionality.
  *
  * This doesn't parse HTTP yet - it just accepts connections and logs them. You
  * can test it with: curl http://localhost:9000
  *
  * The server will accept the connection, log it, and close it. curl will hang
  * and eventually timeout, which is expected at this phase.
  */
object SocketServerTest {

  def main(args: Array[String]): Unit = {
    val server = SocketServer.withShutdownHook(port = 9000)
    println("Starting SocketServer test on port 9000")
    println("Try: curl http://localhost:9000")
    println("Press Ctrl+C to stop")
    server.start()
  }
}
