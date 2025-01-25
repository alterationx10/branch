---
title: Spider
description: A layer over the built-in Java HttpServer
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-01-25T04:38:00Z
tags: 
  - http
  - server
  - client
---

# Spider

*Oh, what a tangled web we weave when first we practice http*

*Spider* is built on top of the built-in Java `HttpServer`.

## JVM HttpServer

In the context of the `HttpServer`, you build `HttpHandler`s that handles an `HttpExchange` (receiving a request and
returning a response). These handlers are registered with a root URI to the server via an `HttpContext`

When a HTTP request is received, the appropriate `HttpContext` (and handler) is located by finding the context whose
path is the longest matching prefix of the request URI's path. Paths are matched literally, which means that the strings
are compared case sensitively, though there is a `ci` string interpolator to help with case-insensitive matching.

## Spider

Spider encapsulates this via `RequestHandler`s and `ContextHandlers`.

There is a `RequestHandler[I,O]` to extend for handling a route. You will need to use a `Convserion` from `Array[Byte]`
to an Input model `I`, and a similar outgoing conversion for you output model `O`. With these conversions, the
RequestHandler takes care of parsing the input streams from the `HttpExchange` and writing the response to the output
stream. Some conversions for simple types are provided by `import RequestHandler.given`.

```scala 3
trait RequestHandler[I, O](using
                           requestDecoder: Conversion[Array[Byte], I],
                           responseEncoder: Conversion[O, Array[Byte]]
                          )
```

Implementing this trait, you will then need to write the function that will handle a `Request[I]` and return a
`Response[O]`.

Here is an example that returns the string `Aloha`.

```scala 3
import RequestHandler.given

case class GreeterGetter() extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    Response(Map.empty, "Aloha")
  }
}
```

`Request` is a case class that wraps/holds some values based on the request of the `HttpContext`, and `Response`
similarly models the output in a more Scala friendly way.

With all of your request handlers made, we can then use them in a `Contexthandler`.

```scala 3
trait ContextHandler(val path: String) 
```

The main thing to implement here is the `contextRouter`. The `contextRouter` is a `PartialFunction` that matches the
http method/verb and request path and maps to a specific `Requesthandler`.

Here is an example:

```scala 3

case class EchoGetter(msg: String) extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    Response(msg)
  }
}

val myhandler = new ContextHandler("/") {

  override val contextRouter
  : PartialFunction[(HttpVerb, Path), RequestHandler[?, ?]] = {
    case HttpVerb.GET -> >> / "some" / "path" => alohaGreeter
    case HttpVerb.GET -> >> / "some" / "path" / s"$arg" => EchoGetter(arg)
  }

}
```

We can then register out `ContextHandler` to an instance of the http server

```scala 3
ContextHandler.registerHandler(myhandler)(using httpServer: HttpServer)
```

`ContextHandler`s support `Filter`s (what might typically be described as middleware, that ge process in the
request/response chain), as well as the `Authenticator` class. These are specific to the root path the `ContextHandler`
is bound to, and this could help determine when to group things into multiple `ContextHandler`s.

There is an `HttpApp` trait that sets up the server for you in an entry point. A quick example:

```scala 3
object HttpAppExample extends HttpApp {

  import RequestHandler.given

  case class GreeterGetter() extends RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response("Aloha")
    }
  }

  val alohaGreeter = GreeterGetter()

  case class EchoGetter(msg: String) extends RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response(msg)
    }
  }

  val myhandler = new ContextHandler("/") {

    override val filters: Seq[Filter] = Seq(
      ContextHandler.timingFilter
    )

    override val contextRouter
    : PartialFunction[(HttpVerb, Path), RequestHandler[?, ?]] = {
      case HttpVerb.GET -> >> / "some" / "path" => alohaGreeter
      case HttpVerb.GET -> >> / "some" / "path" / s"$arg" => EchoGetter(arg)
    }

  }

  ContextHandler.registerHandler(myhandler)
}
```

## Other Libraries

If you like *Spider*, you should check out [Tapir](https://tapir.softwaremill.com/en/latest/)