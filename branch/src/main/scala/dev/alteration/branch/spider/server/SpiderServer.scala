package dev.alteration.branch.spider.server

import dev.alteration.branch.macaroni.runtimes.BranchExecutors
import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.server.RequestHandler.given
import dev.alteration.branch.spider.server.FileHandler.given
import dev.alteration.branch.spider.websocket.{
  WebSocketConnection,
  WebSocketHandler,
  WebSocketHandshake
}
import dev.alteration.branch.friday.http.JsonBody

import java.io.{File, OutputStream}
import java.net.{ServerSocket, Socket, SocketTimeoutException}
import java.util.concurrent.atomic.AtomicBoolean
import scala.util.{Failure, Success, Try, Using}

/** A simple HTTP/1.1 server built directly on ServerSocket.
  *
  * This server uses blocking I/O with virtual threads for handling concurrent
  * connections. Each accepted connection is dispatched to the executor service
  * for processing.
  *
  * Supports both HTTP and WebSocket connections. WebSocket connections are
  * established via HTTP upgrade and then handed off to WebSocketHandlers.
  *
  * @param port
  *   The port on which the server will listen
  * @param backlog
  *   The maximum number of pending connections the server will queue
  * @param router
  *   A partial function to route HTTP requests to handlers
  * @param webSocketRouter
  *   A map from path to WebSocket handler
  * @param config
  *   Server configuration with limits and settings
  */
class SpiderServer(
    val port: Int = 9000,
    val backlog: Int = 0,
    val router: PartialFunction[
      (HttpMethod, List[String]),
      RequestHandler[?, ?]
    ] = PartialFunction.empty,
    val streamingRouter: PartialFunction[
      (HttpMethod, List[String]),
      StreamingRequestHandler[?]
    ] = PartialFunction.empty,
    val webSocketRouter: Map[String, WebSocketHandler] = Map.empty,
    val config: ServerConfig = ServerConfig.default
) extends AutoCloseable {

  private val running                            = new AtomicBoolean(false)
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
        socket.setSoTimeout(config.socketTimeout)

        executor.submit(new Runnable {
          override def run(): Unit = handleConnection(socket)
        })
      }
    } catch {
      case _: java.net.SocketException if !running.get() =>
        // Socket closed during shutdown, this is expected
        println("Server socket closed")
      case e: Exception                                  =>
        println(s"Error in accept loop: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  /** Handle an individual client connection.
    *
    * This method is called on a virtual thread for each accepted connection. It
    * supports HTTP/1.1 keep-alive connections and WebSocket upgrades.
    *
    * @param socket
    *   The client socket to handle
    */
  private def handleConnection(socket: Socket): Unit = {
    Using(socket) { sock =>
      var keepAlive = true

      while (keepAlive && !sock.isClosed) {
        try {
          val input  = sock.getInputStream
          val output = sock.getOutputStream

          // First, parse only headers to determine the route
          HttpParser.parseHeadersOnly(input, config) match {
            case Failure(_: HttpParser.ConnectionClosedException) =>
              // Client closed connection gracefully - exit keep-alive loop
              keepAlive = false

            case Failure(e: HttpParser.LimitExceededException) =>
              // Request exceeded limits, send 413 Payload Too Large or 431 Headers Too Large
              println(s"Request exceeded limits: ${e.getMessage}")
              val errorResponse =
                Response(413, s"Request Too Large: ${e.getMessage}").htmlContent
              HttpWriter.write(errorResponse, output)
              keepAlive = false

            case Success(headersResult) =>
              // Check if this is a WebSocket upgrade request
              val isWebSocketUpgrade =
                isWebSocketUpgradeRequest(headersResult.headers)

              if (isWebSocketUpgrade) {
                // Handle WebSocket upgrade - need to convert to old ParseResult format
                val parseResult = HttpParser.ParseResult(
                  headersResult.method,
                  headersResult.uri,
                  headersResult.httpVersion,
                  headersResult.headers,
                  Array.empty[Byte]
                )
                handleWebSocketUpgrade(sock, parseResult)
                keepAlive = false // WebSocket takes over, exit HTTP loop
              } else {
                // Determine route from headers
                val pathSegments =
                  headersResult.uri.getPath.split("/").toList.filter(_.nonEmpty)
                val routeKey     = headersResult.method -> pathSegments

                val isStreamingHandler = streamingRouter.isDefinedAt(routeKey)

                val handlerResult = if (isStreamingHandler) {
                  // Handle streaming request - pass the buffered stream
                  handleStreamingRequest(headersResult, output, routeKey)
                } else {
                  // Handle regular buffered request - read body from buffered stream
                  handleBufferedRequest(headersResult, output, routeKey)
                }

                handlerResult match {
                  case Success((statusCode, Success(_))) =>
                    // Successfully handled and wrote response
                    keepAlive =
                      shouldKeepAlive(headersResult.headers, statusCode)

                  case Success((_, Failure(writeError))) =>
                    println(s"Error writing response: ${writeError.getMessage}")
                    keepAlive = false

                  case Failure(handlerError) =>
                    // Handler threw an exception, return 500
                    println(s"Handler error: ${handlerError.getMessage}")
                    val errorResponse =
                      internalServerError(handlerError.getMessage)
                    HttpWriter.write(errorResponse, output)
                    keepAlive = false
                }
              }

            case Failure(e) =>
              // Failed to parse request, send 400 Bad Request
              println(s"Failed to parse request: ${e.getMessage}")
              val errorResponse =
                badRequest(s"Invalid HTTP request: ${e.getMessage}")
              HttpWriter.write(errorResponse, output)
              keepAlive = false
          }
        } catch {
          case _: SocketTimeoutException =>
            println("Socket read timeout")
            keepAlive = false
          case e: Exception              =>
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

  /** Handle a regular buffered HTTP request.
    *
    * @param headersResult
    *   The parsed headers with buffered stream
    * @param output
    *   The output stream to write the response to
    * @param routeKey
    *   The route key (method, path segments)
    * @return
    *   Try containing (statusCode, writeResult)
    */
  private def handleBufferedRequest(
      headersResult: HttpParser.HeadersOnlyResult,
      output: OutputStream,
      routeKey: (HttpMethod, List[String])
  ): Try[(Int, Try[Unit])] = {
    Try {
      // Route to appropriate handler
      val handler = router
        .lift(routeKey)
        .getOrElse(RequestHandler.notFoundHandler)

      // Read the body now (buffered) from the BufferedInputStream
      val body = HttpParser.readBodyFromStream(
        headersResult.bufferedInput,
        headersResult.headers,
        config
      ) match {
        case Success(bodyBytes) => bodyBytes
        case Failure(e)         => throw e
      }

      // Create full ParseResult with body
      val parseResult = HttpParser.ParseResult(
        headersResult.method,
        headersResult.uri,
        headersResult.httpVersion,
        headersResult.headers,
        body
      )

      // Convert to Request model
      val request = HttpParser.toRequest(parseResult)

      val response = handler
        .asInstanceOf[RequestHandler[Array[Byte], Any]]
        .handle(request)

      // Pattern match on common response types
      val writeResult = response.body match {
        case _: StreamingResponse  =>
          // Handle streaming responses specially
          HttpWriter.writeStreamingResponse(
            response.asInstanceOf[Response[StreamingResponse]],
            output
          )
        case _: String             =>
          HttpWriter.write(
            response.asInstanceOf[Response[String]],
            output
          )
        case _: Array[Byte]        =>
          HttpWriter.write(
            response.asInstanceOf[Response[Array[Byte]]],
            output
          )
        case _: File               =>
          HttpWriter.write(
            response.asInstanceOf[Response[File]],
            output
          )
        case jsonBody: JsonBody[?] =>
          // JsonBody wraps bytes, extract them and write
          val bytesResponse = Response(
            response.statusCode,
            jsonBody.bytes,
            response.headers
          )
          HttpWriter.write(bytesResponse, output)
        case _                     =>
          // Fallback: convert to String
          val stringResponse = Response(
            response.statusCode,
            response.body.toString,
            response.headers
          )
          HttpWriter.write(stringResponse, output)
      }

      (response.statusCode, writeResult)
    }
  }

  /** Handle a streaming HTTP request.
    *
    * @param headersResult
    *   The parsed headers with buffered stream
    * @param output
    *   The output stream to write the response to
    * @param routeKey
    *   The route key (method, path segments)
    * @return
    *   Try containing (statusCode, writeResult)
    */
  private def handleStreamingRequest(
      headersResult: HttpParser.HeadersOnlyResult,
      output: OutputStream,
      routeKey: (HttpMethod, List[String])
  ): Try[(Int, Try[Unit])] = {
    Try {
      val handler = streamingRouter(routeKey)

      // Create StreamingRequest from the buffered stream (body not buffered yet)
      val streamingRequestBody = handler.createStreamingRequest(
        headersResult.bufferedInput,
        headersResult.headers,
        config
      )

      // Create Request with StreamingRequest body
      val request = Request(
        uri = headersResult.uri,
        headers = headersResult.headers,
        body = streamingRequestBody
      )

      // Handle the request
      val response = handler
        .asInstanceOf[StreamingRequestHandler[Any]]
        .handle(request)

      // Write response (similar to buffered handling)
      val writeResult = response.body match {
        case _: StreamingResponse =>
          HttpWriter.writeStreamingResponse(
            response.asInstanceOf[Response[StreamingResponse]],
            output
          )
        case _: String            =>
          HttpWriter.write(
            response.asInstanceOf[Response[String]],
            output
          )
        case _: Array[Byte]       =>
          HttpWriter.write(
            response.asInstanceOf[Response[Array[Byte]]],
            output
          )
        case _: File              =>
          HttpWriter.write(
            response.asInstanceOf[Response[File]],
            output
          )
        case _                    =>
          val stringResponse = Response(
            response.statusCode,
            response.body.toString,
            response.headers
          )
          HttpWriter.write(stringResponse, output)
      }

      (response.statusCode, writeResult)
    }
  }

  /** Check if a request is a WebSocket upgrade request.
    *
    * @param headers
    *   The request headers
    * @return
    *   true if this is a WebSocket upgrade request
    */
  private def isWebSocketUpgradeRequest(
      headers: Map[String, List[String]]
  ): Boolean = {
    // Look for Upgrade: websocket header (case-insensitive)
    headers
      .find { case (k, _) => k.equalsIgnoreCase("Upgrade") }
      .flatMap(_._2.headOption)
      .exists(_.equalsIgnoreCase("websocket"))
  }

  /** Handle a WebSocket upgrade request.
    *
    * Validates the handshake, sends 101 Switching Protocols, and hands off to
    * the appropriate WebSocketHandler.
    *
    * @param socket
    *   The client socket
    * @param parseResult
    *   The parsed HTTP request
    */
  private def handleWebSocketUpgrade(
      socket: Socket,
      parseResult: HttpParser.ParseResult
  ): Unit = {
    val path   = parseResult.uri.getPath
    val output = socket.getOutputStream

    // Route to WebSocket handler
    webSocketRouter.get(path) match {
      case None =>
        // No handler for this path, send 404
        println(s"No WebSocket handler for path: $path")
        val errorResponse =
          Response(404, s"No WebSocket handler for path: $path").htmlContent
        HttpWriter.write(errorResponse, output)

      case Some(handler) =>
        // Validate WebSocket handshake
        WebSocketHandshake.validateHandshake(parseResult.headers) match {
          case Failure(error) =>
            // Invalid handshake, send 400
            println(s"Invalid WebSocket handshake: ${error.getMessage}")
            val errorResponse = badRequest(
              s"Invalid WebSocket handshake: ${error.getMessage}"
            )
            HttpWriter.write(errorResponse, output)

          case Success(secWebSocketKey) =>
            // Send 101 Switching Protocols response
            val responseBytes =
              WebSocketHandshake.createRawHandshakeResponse(secWebSocketKey)
            output.write(responseBytes)
            output.flush()

            println(s"WebSocket connection established on $path")

            // Create WebSocketConnection and hand off to handler
            val connection = WebSocketConnection(socket)

            try {
              // Call handler lifecycle methods
              WebSocketHandler.handleConnection(handler, connection)
            } catch {
              case e: Exception =>
                println(s"Error in WebSocket handler: ${e.getMessage}")
                e.printStackTrace()
            }
        }
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

  /** Close the server (implements AutoCloseable).
    *
    * This calls stop() to gracefully shut down the server.
    */
  override def close(): Unit = stop()

  /** Check if the server is currently running.
    */
  def isRunning: Boolean = running.get()
}

object SpiderServer {

  /** Create a SocketServer with a shutdown hook that stops the server on JVM
    * shutdown.
    *
    * @param port
    *   The port on which the server will listen
    * @param backlog
    *   The maximum number of pending connections
    * @param router
    *   A partial function to route HTTP requests to handlers
    * @param webSocketRouter
    *   A map from path to WebSocket handler
    * @param config
    *   Server configuration with limits and settings
    * @return
    *   A new SocketServer instance with shutdown hook installed
    */
  def withShutdownHook(
      port: Int = 9000,
      backlog: Int = 0,
      router: PartialFunction[
        (HttpMethod, List[String]),
        RequestHandler[?, ?]
      ] = PartialFunction.empty,
      streamingRouter: PartialFunction[
        (HttpMethod, List[String]),
        StreamingRequestHandler[?]
      ] = PartialFunction.empty,
      webSocketRouter: Map[String, WebSocketHandler] = Map.empty,
      config: ServerConfig = ServerConfig.default
  ): SpiderServer = {
    val server =
      new SpiderServer(
        port,
        backlog,
        router,
        streamingRouter,
        webSocketRouter,
        config
      )

    Runtime.getRuntime.addShutdownHook {
      new Thread(() => server.stop())
    }

    server
  }
}
