package dev.alteration.branch.spider.websocket

import dev.alteration.branch.macaroni.runtimes.BranchExecutors

/** Standalone WebSocket server examples using WebSocketServer.
  *
  * These examples don't integrate with HttpServer - they listen on their own
  * ports for WebSocket connections only.
  */
object StandaloneEchoExample {

  given scala.concurrent.ExecutionContext = BranchExecutors.executionContext

  def main(args: Array[String]): Unit = {
    val handler = new WebSocketHandler {
      override def onConnect(connection: WebSocketConnection): Unit = {
        println("[Echo] Client connected")
        connection.sendText("Welcome to the standalone echo server!")
      }

      override def onMessage(connection: WebSocketConnection, message: String): Unit = {
        println(s"[Echo] Received: $message")
        connection.sendText(s"Echo: $message")
      }

      override def onClose(
          connection: WebSocketConnection,
          statusCode: Option[Int],
          reason: String
      ): Unit = {
        println(s"[Echo] Connection closed: ${statusCode.getOrElse("no code")}")
      }

      override def onError(connection: WebSocketConnection, error: Throwable): Unit = {
        println(s"[Echo] Error: ${error.getMessage}")
        error.printStackTrace()
      }
    }

    val server = new WebSocketServer(9002, handler)
    println("Starting standalone WebSocket echo server on ws://localhost:9002/")
    println("Press Ctrl+C to stop")

    Runtime.getRuntime.addShutdownHook(new Thread(() => server.stop()))

    server.start()
  }

}

object StandaloneChatExample {

  given scala.concurrent.ExecutionContext = BranchExecutors.executionContext

  def main(args: Array[String]): Unit = {
    val connections = scala.collection.mutable.Set.empty[WebSocketConnection]
    val lock        = new Object()

    def broadcast(message: String): Unit = {
      lock.synchronized {
        connections.foreach { conn =>
          if (conn.isOpen) {
            conn.sendText(message).failed.foreach { error =>
              println(s"[Chat] Failed to send: ${error.getMessage}")
            }
          }
        }
      }
    }

    val handler = new WebSocketHandler {
      override def onConnect(connection: WebSocketConnection): Unit = {
        lock.synchronized {
          connections += connection
        }
        println(s"[Chat] Client connected. Total: ${connections.size}")
        broadcast(s"User joined. Total users: ${connections.size}")
      }

      override def onMessage(connection: WebSocketConnection, message: String): Unit = {
        println(s"[Chat] Broadcasting: $message")
        broadcast(message)
      }

      override def onClose(
          connection: WebSocketConnection,
          statusCode: Option[Int],
          reason: String
      ): Unit = {
        lock.synchronized {
          connections -= connection
        }
        println(s"[Chat] Client disconnected. Total: ${connections.size}")
        broadcast(s"User left. Total users: ${connections.size}")
      }

      override def onError(connection: WebSocketConnection, error: Throwable): Unit = {
        println(s"[Chat] Error: ${error.getMessage}")
        lock.synchronized {
          connections -= connection
        }
      }
    }

    val server = new WebSocketServer(9003, handler)
    println("Starting standalone WebSocket chat server on ws://localhost:9003/")
    println("Press Ctrl+C to stop")

    Runtime.getRuntime.addShutdownHook(new Thread(() => server.stop()))

    server.start()
  }

}
