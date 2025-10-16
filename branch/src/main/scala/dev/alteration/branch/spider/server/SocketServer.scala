package dev.alteration.branch.spider.server

import dev.alteration.branch.macaroni.runtimes.BranchExecutors

import java.net.{ServerSocket, Socket}
import java.util.concurrent.atomic.AtomicBoolean
import scala.util.{Try, Using}

/** A simple HTTP server built directly on ServerSocket.
  *
  * This server uses blocking I/O with virtual threads for handling concurrent
  * connections. Each accepted connection is dispatched to the executor service
  * for processing.
  *
  * @param port
  *   The port on which the server will listen
  * @param backlog
  *   The maximum number of pending connections the server will queue
  */
class SocketServer(val port: Int = 9000, val backlog: Int = 0) {

  private val running = new AtomicBoolean(false)
  private var serverSocket: Option[ServerSocket] = None

  /** The executor service used for handling connections. Uses virtual threads
    * for lightweight concurrency with blocking I/O.
    */
  private val executor = BranchExecutors.executorService

  /** Start the server and begin accepting connections.
    *
    * This method blocks and runs the accept loop on the current thread. Each
    * accepted connection is dispatched to a virtual thread for handling.
    */
  def start(): Unit = {
    if (!running.compareAndSet(false, true)) {
      println("Server is already running")
      return
    }

    serverSocket = Some(new ServerSocket(port, backlog))
    println(s"SocketServer listening on port $port")

    try {
      while (running.get()) {
        val socket = serverSocket.get.accept()
        executor.submit(new Runnable {
          override def run(): Unit = handleConnection(socket)
        })
      }
    } catch {
      case _: java.net.SocketException if !running.get() =>
        // Socket closed during shutdown, this is expected
        println("Server socket closed")
      case e: Exception =>
        println(s"Error in accept loop: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  /** Handle an individual client connection.
    *
    * This method is called on a virtual thread for each accepted connection.
    * Currently a placeholder - will be wired to HTTP parsing in Phase 2.
    *
    * @param socket
    *   The client socket to handle
    */
  private def handleConnection(socket: Socket): Unit = {
    Using(socket) { sock =>
      // TODO: Phase 2 - HTTP request parsing will go here
      // For now, just acknowledge we got a connection
      println(s"Handling connection from ${sock.getRemoteSocketAddress}")

      // Placeholder: read some data and close
      val input = sock.getInputStream
      val available = input.available()
      if (available > 0) {
        val buffer = new Array[Byte](Math.min(available, 1024))
        input.read(buffer)
        println(s"Received ${buffer.length} bytes")
      }
    } match {
      case scala.util.Success(_) =>
        // Connection handled successfully
      case scala.util.Failure(e) =>
        println(s"Error handling connection: ${e.getMessage}")
    }
  }

  /** Stop the server and close the server socket.
    *
    * This will cause the accept loop to exit and clean up resources. The
    * executor service is NOT shut down (it's global).
    */
  def stop(): Unit = {
    if (running.compareAndSet(true, false)) {
      println("Stopping server...")
      serverSocket.foreach { sock =>
        Try(sock.close())
      }
      serverSocket = None
      println("Server stopped")
    }
  }

  /** Check if the server is currently running.
    */
  def isRunning: Boolean = running.get()
}

object SocketServer {

  /** Create a SocketServer with a shutdown hook that stops the server on JVM
    * shutdown.
    *
    * @param port
    *   The port on which the server will listen
    * @param backlog
    *   The maximum number of pending connections
    * @return
    *   A new SocketServer instance with shutdown hook installed
    */
  def withShutdownHook(port: Int = 9000, backlog: Int = 0): SocketServer = {
    val server = new SocketServer(port, backlog)

    Runtime.getRuntime.addShutdownHook {
      new Thread(() => server.stop())
    }

    server
  }
}
