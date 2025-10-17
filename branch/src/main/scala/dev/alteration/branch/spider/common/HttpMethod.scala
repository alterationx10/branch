package dev.alteration.branch.spider.common

import scala.util.Try

/** Enum for HTTP methods
  */
enum HttpMethod {
  case GET, HEAD, OPTIONS, TRACE, PUT, DELETE, POST, PATCH, CONNECT
}

object HttpMethod {

  /** Convert a string to an HttpMethod
    * @param method
    * @return
    */
  def fromString(method: String): Option[HttpMethod] =
    Try(HttpMethod.valueOf(method)).toOption

}
