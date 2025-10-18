package dev.alteration.branch.spider.server

import java.net.URI

/** A request model for an HTTP Request */
case class Request[A](uri: URI, headers: Map[String, List[String]], body: A)

object Request {

  /** Parse query parameters from a query string */
  def parseQueryParams(qpStr: String): Map[String, String] = {
    qpStr
      .split("&")
      .map { case s"$key=$value" =>
        key -> value
      }
      .toMap
  }

  extension [A](r: Request[A]) {

    /** Get query parameters from the request URI */
    def queryParams: Map[String, String] = parseQueryParams(r.uri.getQuery)

    /** Get all cookies from the request as a Map of name -> value */
    def cookies: Map[String, String] = Cookie.fromHeaders(r.headers)

    /** Get a specific cookie value by name */
    def cookie(name: String): Option[String] = cookies.get(name)
  }

}
