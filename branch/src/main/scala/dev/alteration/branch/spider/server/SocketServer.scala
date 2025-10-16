package dev.alteration.branch.spider.server

import dev.alteration.branch.macaroni.runtimes.BranchExecutors
import dev.alteration.branch.spider.HttpMethod
import dev.alteration.branch.spider.server.RequestHandler.given

import java.net.{ServerSocket, Socket, SocketTimeoutException}
import java.util.concurrent.atomic.AtomicBoolean
import scala.util.{Try, Using, Success, Failure}

/** A simple HTTP/1.1 server built directly on ServerSocket.
  *
  * This server uses blocking I/O with virtual threads for handling concurrent
  * connections. Each accepted connection is dispatched to the executor service
  * for processing.
  *
  * @param port
  *   The port on which the server will listen
  * @param backlog
  *   The maximum number of pending connections the server will queue
  * @param router
  *   A partial function to route requests to handlers
  * @param socketTimeout
  *   Read timeout for client connections in milliseconds (default 30s)
  */
class SocketServer(
    val port: Int = 9000,
    val backlog: Int = 0,
    val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = PartialFunction.empty,
    val socketTimeout: Int = 30000
) {

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
        socket.setSoTimeout(socketTimeout)

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
    * It supports HTTP/1.1 keep-alive connections.
    *
    * @param socket
    *   The client socket to handle
    */
  private def handleConnection(socket: Socket): Unit = {
    Using(socket) { sock =>
      var keepAlive = true

      while (keepAlive && !sock.isClosed) {
        try {
          val input = sock.getInputStream
          val output = sock.getOutputStream

          // Parse the HTTP request
          HttpParser.parse(input) match {
            case Failure(_: HttpParser.ConnectionClosedException) =>
              // Client closed connection gracefully - exit keep-alive loop
              keepAlive = false

            case Success(parseResult) =>
              // Route to appropriate handler
              val handler = routeRequest(parseResult.method, parseResult.uri.getPath)

              // Convert to Request model
              val request = HttpParser.toRequest(parseResult)

              // Execute handler - we'll handle type erasure by writing raw bytes
              val handlerResult = Try {
                val response = handler.asInstanceOf[RequestHandler[Array[Byte], Any]].handle(request)

                // Pattern match on common response types
                val writeResult = response.body match {
                  case s: String =>
                    HttpWriter.write(response.asInstanceOf[Response[String]], output)
                  case bytes: Array[Byte] =>
                    HttpWriter.write(response.asInstanceOf[Response[Array[Byte]]], output)
                  case _ =>
                    // Fallback: convert to String
                    val stringResponse = Response(response.statusCode, response.body.toString, response.headers)
                    HttpWriter.write(stringResponse, output)
                }

                (response.statusCode, writeResult)
              }

              handlerResult match {
                case Success((statusCode, Success(_))) =>
                  // Successfully handled and wrote response
                  keepAlive = shouldKeepAlive(parseResult.headers, statusCode)

                case Success((_, Failure(writeError))) =>
                  println(s"Error writing response: ${writeError.getMessage}")
                  keepAlive = false

                case Failure(handlerError) =>
                  // Handler threw an exception, return 500
                  println(s"Handler error: ${handlerError.getMessage}")
                  val errorResponse = internalServerError(handlerError.getMessage)
                  HttpWriter.write(errorResponse, output)
                  keepAlive = false
              }

            case Failure(e) =>
              // Failed to parse request, send 400 Bad Request
              println(s"Failed to parse request: ${e.getMessage}")
              val errorResponse = badRequest(s"Invalid HTTP request: ${e.getMessage}")
              HttpWriter.write(errorResponse, output)
              keepAlive = false
          }
        } catch {
          case _: SocketTimeoutException =>
            println("Socket read timeout")
            keepAlive = false
          case e: Exception =>
            println(s"Error processing request: ${e.getMessage}")
            keepAlive = false
        }
      }
    } match {
      case Success(_) =>
        // Connection handled successfully
      case Failure(e) =>
        println(s"Error handling connection: ${e.getMessage}")
    }
  }

  /** Route a request to the appropriate handler.
    *
    * @param method
    *   The HTTP method
    * @param path
    *   The request path
    * @return
    *   The handler to use (or notFoundHandler if no match)
    */
  private def routeRequest(
      method: HttpMethod,
      path: String
  ): RequestHandler[?, ?] = {
    val pathSegments = path.split("/").toList.filter(_.nonEmpty)

    router
      .lift(method -> pathSegments)
      .getOrElse(RequestHandler.notFoundHandler)
  }

  /** Determine if the connection should be kept alive.
    *
    * HTTP/1.1 defaults to keep-alive unless "Connection: close" is present.
    *
    * @param requestHeaders
    *   The request headers
    * @param statusCode
    *   The response status code
    * @return
    *   true if connection should stay open
    */
  private def shouldKeepAlive(
      requestHeaders: Map[String, List[String]],
      statusCode: Int
  ): Boolean = {
    // Don't keep alive on error responses
    if (statusCode >= 400) return false

    // Check for explicit "Connection: close"
    val connectionHeader = requestHeaders
      .find { case (k, _) => k.equalsIgnoreCase("Connection") }
      .flatMap(_._2.headOption)
      .map(_.toLowerCase)

    connectionHeader match {
      case Some("close") => false
      case _             => true // HTTP/1.1 defaults to keep-alive
    }
  }

  /** Generate a 400 Bad Request response.
    */
  private def badRequest(message: String): Response[String] = {
    Response(400, message).htmlContent
  }

  /** Generate a 500 Internal Server Error response.
    */
  private def internalServerError(message: String): Response[String] = {
    Response(500, s"Internal Server Error: $message").htmlContent
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
    * @param router
    *   A partial function to route requests to handlers
    * @param socketTimeout
    *   Read timeout for client connections in milliseconds
    * @return
    *   A new SocketServer instance with shutdown hook installed
    */
  def withShutdownHook(
      port: Int = 9000,
      backlog: Int = 0,
      router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = PartialFunction.empty,
      socketTimeout: Int = 30000
  ): SocketServer = {
    val server = new SocketServer(port, backlog, router, socketTimeout)

    Runtime.getRuntime.addShutdownHook {
      new Thread(() => server.stop())
    }

    server
  }
}
