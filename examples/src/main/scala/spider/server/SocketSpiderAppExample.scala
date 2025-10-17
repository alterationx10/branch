package spider.server

import dev.alteration.branch.spider.server.{SocketSpiderApp, RequestHandler, Request, Response}
import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.server.RequestHandler.given
import dev.alteration.branch.spider.server.Response.*

/** Example application using SocketSpiderApp.
  *
  * This demonstrates how to use the SocketServer-based infrastructure with
  * a simple router pattern.
  *
  * Test with:
  *   curl http://localhost:9000/
  *   curl http://localhost:9000/api/status
  *   curl http://localhost:9000/api/users
  *   curl http://localhost:9000/admin/dashboard
  *   curl http://localhost:9000/notfound
  */
object SocketSpiderAppExample extends SocketSpiderApp {

  // API handlers
  val statusHandler = new RequestHandler[Array[Byte], String] {
    override def handle(request: Request[Array[Byte]]): Response[String] = {
      json"""
      {
        "status": "ok",
        "version": "1.0.0",
        "server": "SocketServer"
      }
      """
    }
  }

  val usersHandler = new RequestHandler[Array[Byte], String] {
    override def handle(request: Request[Array[Byte]]): Response[String] = {
      json"""
      {
        "users": [
          {"id": 1, "name": "Alice"},
          {"id": 2, "name": "Bob"}
        ]
      }
      """
    }
  }

  // Admin handler
  val dashboardHandler = new RequestHandler[Array[Byte], String] {
    override def handle(request: Request[Array[Byte]]): Response[String] = {
      html"""
      <h1>Admin Dashboard</h1>
      <p>Welcome to the admin panel</p>
      <ul>
        <li>Total Users: 42</li>
        <li>Active Sessions: 12</li>
      </ul>
      """
    }
  }

  // Home handler
  val homeHandler = new RequestHandler[Array[Byte], String] {
    override def handle(request: Request[Array[Byte]]): Response[String] = {
      html"""
      <h1>Welcome to SocketSpiderApp</h1>
      <p>Available endpoints:</p>
      <ul>
        <li><a href="/api/status">/api/status</a> - API status</li>
        <li><a href="/api/users">/api/users</a> - User list</li>
        <li><a href="/admin/dashboard">/admin/dashboard</a> - Admin dashboard</li>
      </ul>
      """
    }
  }

  // Define the router - maps (HttpMethod, path segments) to handlers
  override val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
    // Root
    case (HttpMethod.GET, Nil) => homeHandler

    // API routes
    case (HttpMethod.GET, "api" :: "status" :: Nil) => statusHandler
    case (HttpMethod.GET, "api" :: "users" :: Nil)  => usersHandler

    // Admin routes
    case (HttpMethod.GET, "admin" :: "dashboard" :: Nil) => dashboardHandler
  }

  // Port is inherited from SocketSpiderApp (default 9000)
  // Can override: override val port = 8080
}
