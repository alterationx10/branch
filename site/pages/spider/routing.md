---
title: Spider Advanced Routing
description: Path parameters, query strings, and routing helpers
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-01-25T04:38:00Z
tags:
  - http
  - routing
  - server
---

# Advanced Routing

Spider provides powerful routing capabilities including path parameter extraction, query string parsing, and routing helpers.

## Path Parameters

Extract typed values from URL paths using pattern matching extractors:

```scala
import dev.alteration.branch.spider.server.RoutingHelpers.*
import dev.alteration.branch.spider.common.HttpMethod

val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
  // Integer parameter
  case (HttpMethod.GET, "users" :: IntParam(userId) :: Nil) =>
    UserHandler(userId)  // userId is an Int

  // UUID parameter
  case (HttpMethod.GET, "resources" :: UuidParam(id) :: Nil) =>
    ResourceHandler(id)  // id is a UUID

  // Long parameter
  case (HttpMethod.GET, "posts" :: LongParam(timestamp) :: Nil) =>
    PostHandler(timestamp)  // timestamp is a Long

  // Double parameter
  case (HttpMethod.GET, "prices" :: DoubleParam(amount) :: Nil) =>
    PriceHandler(amount)  // amount is a Double

  // Boolean parameter
  case (HttpMethod.GET, "flags" :: BoolParam(enabled) :: Nil) =>
    FlagHandler(enabled)  // enabled is a Boolean

  // Multiple parameters
  case (HttpMethod.GET, "users" :: IntParam(userId) :: "posts" :: IntParam(postId) :: Nil) =>
    UserPostHandler(userId, postId)
}
```

### Available Extractors

- `IntParam` - Extracts Int values
- `LongParam` - Extracts Long values
- `UuidParam` - Extracts UUID values
- `DoubleParam` - Extracts Double values
- `BoolParam` - Extracts Boolean values ("true"/"false", case-insensitive)

### Custom Regex Extractors

Create custom extractors using regular expressions:

```scala
import scala.util.matching.Regex

// Email extractor
val EmailParam = RegexParam("""[\w.+-]+@[\w.-]+\.\w+""".r)

val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
  case (HttpMethod.GET, "users" :: EmailParam(email) :: Nil) =>
    UserByEmailHandler(email)  // email is a String

  // Slug extractor (lowercase alphanumeric with hyphens)
  val SlugParam = RegexParam("""[a-z0-9-]+""".r)

  case (HttpMethod.GET, "articles" :: SlugParam(slug) :: Nil) =>
    ArticleHandler(slug)
}
```

## Query String Parsing

Parse and extract query parameters:

```scala
import dev.alteration.branch.spider.server.RoutingHelpers.QueryParams

case class SearchHandler() extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    val queryString = request.uri.getQuery
    val params = QueryParams.parse(queryString)

    // Get single value
    val query = QueryParams.get(params, "q")
    // Some("search term") or None

    // Get all values for a key
    val tags = QueryParams.getAll(params, "tag")
    // List("scala", "http") for ?tag=scala&tag=http

    // Get required parameter
    val required = QueryParams.required(params, "api_key")
    // Some("key") or None if missing/empty

    Response(200, s"Search: ${query.getOrElse("none")}")
  }
}
```

### Query Parameter Map

Parsed query parameters are a `Map[String, List[String]]`:

```scala
// URL: /search?q=scala&tag=http&tag=server&limit=10

val params = QueryParams.parse(request.uri.getQuery)
// Map(
//   "q" -> List("scala"),
//   "tag" -> List("http", "server"),
//   "limit" -> List("10")
// )

// Get first value
val query = params.get("q").flatMap(_.headOption)
// Some("scala")

// Get all tags
val tags = params.getOrElse("tag", Nil)
// List("http", "server")
```

## Route Prefixing

Group routes under a common prefix:

```scala
import dev.alteration.branch.spider.server.RoutingHelpers.Routes

// API v1 routes
val apiV1 = Routes.withPrefix("api" :: "v1" :: Nil) {
  case (HttpMethod.GET, "users" :: Nil) => ListUsersHandler()
  case (HttpMethod.POST, "users" :: Nil) => CreateUserHandler()
  case (HttpMethod.GET, "users" :: IntParam(id) :: Nil) => GetUserHandler(id)
}

// API v2 routes
val apiV2 = Routes.withPrefix("api" :: "v2" :: Nil) {
  case (HttpMethod.GET, "users" :: Nil) => ListUsersV2Handler()
  case (HttpMethod.POST, "users" :: Nil) => CreateUserV2Handler()
}

// Combine routers
val router = Routes.combine(apiV1, apiV2)

// Now accessible as:
// GET /api/v1/users
// POST /api/v1/users
// GET /api/v1/users/123
// GET /api/v2/users
// POST /api/v2/users
```

## Method-Specific Routes

Create routes for specific HTTP methods:

```scala
import dev.alteration.branch.spider.server.RoutingHelpers.Routes

// GET routes only
val getRoutes = Routes.get {
  case "users" :: Nil => ListUsersHandler()
  case "users" :: IntParam(id) :: Nil => GetUserHandler(id)
}

// POST routes only
val postRoutes = Routes.post {
  case "users" :: Nil => CreateUserHandler()
  case "users" :: IntParam(id) :: "posts" :: Nil => CreatePostHandler(id)
}

// PUT routes only
val putRoutes = Routes.put {
  case "users" :: IntParam(id) :: Nil => UpdateUserHandler(id)
}

// DELETE routes only
val deleteRoutes = Routes.delete {
  case "users" :: IntParam(id) :: Nil => DeleteUserHandler(id)
}

// Combine all routes
val router = Routes.combine(
  getRoutes,
  postRoutes,
  putRoutes,
  deleteRoutes
)
```

## Combining Routers

Merge multiple routers using `orElse`:

```scala
val usersRouter: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
  case (HttpMethod.GET, "users" :: Nil) => ListUsersHandler()
  case (HttpMethod.GET, "users" :: IntParam(id) :: Nil) => GetUserHandler(id)
}

val postsRouter: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
  case (HttpMethod.GET, "posts" :: Nil) => ListPostsHandler()
  case (HttpMethod.GET, "posts" :: IntParam(id) :: Nil) => GetPostHandler(id)
}

val router = usersRouter orElse postsRouter

// Or using Routes.combine
val router = Routes.combine(usersRouter, postsRouter)
```

## Complete RESTful Example

```scala
import dev.alteration.branch.spider.server.*
import dev.alteration.branch.spider.server.RoutingHelpers.*
import dev.alteration.branch.spider.common.HttpMethod

object MyApp extends SpiderApp {
  override val router = {
    // Users
    case (HttpMethod.GET, "users" :: Nil) =>
      ListUsersHandler()

    case (HttpMethod.POST, "users" :: Nil) =>
      CreateUserHandler()

    case (HttpMethod.GET, "users" :: IntParam(id) :: Nil) =>
      GetUserHandler(id)

    case (HttpMethod.PUT, "users" :: IntParam(id) :: Nil) =>
      UpdateUserHandler(id)

    case (HttpMethod.DELETE, "users" :: IntParam(id) :: Nil) =>
      DeleteUserHandler(id)

    // User's posts
    case (HttpMethod.GET, "users" :: IntParam(userId) :: "posts" :: Nil) =>
      ListUserPostsHandler(userId)

    case (HttpMethod.POST, "users" :: IntParam(userId) :: "posts" :: Nil) =>
      CreateUserPostHandler(userId)

    case (HttpMethod.GET, "users" :: IntParam(userId) :: "posts" :: IntParam(postId) :: Nil) =>
      GetUserPostHandler(userId, postId)

    // Search with query params
    case (HttpMethod.GET, "search" :: Nil) =>
      SearchHandler()

    // Catch-all for 404
    case _ =>
      NotFoundHandler()
  }
}

case class SearchHandler() extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    val query = request.uri.getQuery
    val params = QueryParams.parse(query)

    val searchTerm = QueryParams.get(params, "q")
    val limit = QueryParams.get(params, "limit").flatMap(_.toIntOption).getOrElse(10)
    val offset = QueryParams.get(params, "offset").flatMap(_.toIntOption).getOrElse(0)

    // Perform search...
    Response(200, s"Searching for: ${searchTerm.getOrElse("*")}, limit: $limit, offset: $offset")
  }
}
```

## Modular Routing

Organize routes into modules:

```scala
object UsersRoutes {
  val routes: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
    case (HttpMethod.GET, "users" :: Nil) => ListUsersHandler()
    case (HttpMethod.POST, "users" :: Nil) => CreateUserHandler()
    case (HttpMethod.GET, "users" :: IntParam(id) :: Nil) => GetUserHandler(id)
    case (HttpMethod.PUT, "users" :: IntParam(id) :: Nil) => UpdateUserHandler(id)
    case (HttpMethod.DELETE, "users" :: IntParam(id) :: Nil) => DeleteUserHandler(id)
  }
}

object PostsRoutes {
  val routes: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
    case (HttpMethod.GET, "posts" :: Nil) => ListPostsHandler()
    case (HttpMethod.POST, "posts" :: Nil) => CreatePostHandler()
    case (HttpMethod.GET, "posts" :: IntParam(id) :: Nil) => GetPostHandler(id)
  }
}

object ApiRoutes {
  val routes: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] =
    Routes.withPrefix("api" :: "v1" :: Nil) {
      Routes.combine(
        UsersRoutes.routes,
        PostsRoutes.routes
      )
    }
}

object MyApp extends SpiderApp {
  override val router = ApiRoutes.routes orElse {
    case _ => NotFoundHandler()
  }
}
```

## Wildcard and Catch-All Routes

Match remaining path segments:

```scala
val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
  // Exact match
  case (HttpMethod.GET, "api" :: "users" :: Nil) =>
    ListUsersHandler()

  // Match with remaining segments
  case (HttpMethod.GET, "files" :: path) =>
    FileHandler(path)  // path is List[String] with remaining segments

  // Catch-all for 404
  case (method, path) =>
    NotFoundHandler(method, path.mkString("/"))
}

case class FileHandler(pathSegments: List[String]) extends RequestHandler[Unit, Array[Byte]] {
  override def handle(request: Request[Unit]): Response[Array[Byte]] = {
    val filePath = pathSegments.mkString("/")
    // Serve file from /files/...
    serveFile(s"/static/$filePath")
  }
}
```

## Route Testing

Test routes with pattern matching:

```scala
val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
  case (HttpMethod.GET, "users" :: IntParam(id) :: Nil) => GetUserHandler(id)
}

// Check if route is defined
val isDefinedForUser = router.isDefinedAt((HttpMethod.GET, List("users", "123")))
// true

val isDefinedForInvalid = router.isDefinedAt((HttpMethod.GET, List("users", "abc")))
// false (IntParam doesn't match "abc")

// Get handler
val handler = router.lift((HttpMethod.GET, List("users", "123")))
// Some(GetUserHandler(123))
```

## Best Practices

1. **Use extractors**: Leverage `IntParam`, `UuidParam`, etc. for type-safe routing
2. **Organize by resource**: Group related routes together
3. **Prefix API versions**: Use `Routes.withPrefix` for versioning
4. **Method-specific routers**: Use `Routes.get`, `Routes.post`, etc. for clarity
5. **Catch-all last**: Always place catch-all routes at the end
6. **Validate parameters**: Don't trust extracted values, validate in handlers
7. **Query params**: Parse query strings for filtering, pagination, etc.

## Next Steps

- Learn about [Middleware](middleware.md) for request processing
- Explore [Body Parsing](body-parsing.md) for handling request bodies
- Return to [HTTP Server](server.md)
