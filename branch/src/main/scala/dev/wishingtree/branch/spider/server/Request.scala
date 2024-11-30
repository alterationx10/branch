package dev.wishingtree.branch.spider.server

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
  }

}
