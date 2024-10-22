package dev.wishingtree.branch.http

// https: //developer.mozilla.org/en-US/docs/Web/HTTP/Methods
enum HttpVerb {
  case GET, HEAD, OPTIONS, TRACE, PUT, DELETE, POST, PATCH, CONNECT
}

object HttpVerb {
  private val lookup: Map[String, HttpVerb] =
    HttpVerb.values.map(v => v.toString -> v).toMap

  def fromString(method: String): Option[HttpVerb] =
    lookup.get(method)
}
