package dev.wishingtree.branch.spider.server

import dev.wishingtree.branch.spider.server.HttpVerb

import scala.util.Try

// https: //developer.mozilla.org/en-US/docs/Web/HTTP/Methods
enum HttpVerb {
  case GET, HEAD, OPTIONS, TRACE, PUT, DELETE, POST, PATCH, CONNECT
}

object HttpVerb {

  def fromString(method: String): Option[HttpVerb] =
    Try(HttpVerb.valueOf(method)).toOption

}
