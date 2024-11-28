package dev.wishingtree.branch.spider.server

import java.net.URI

case class Request[A](uri: URI, headers: Map[String, List[String]], body: A)

object Request {

  def parseQueryParams(qpStr: String): Map[String, String] = {
    qpStr
      .split("&")
      .map { case s"$key=$value" =>
        key -> value
      }
      .toMap
  }

  extension [A](r: Request[A]) {
    def queryParams: Map[String, String] = parseQueryParams(r.uri.getQuery)
  }

}
