package dev.alteration.branch.spider.server

import dev.alteration.branch.spider.common.HttpMethod
import java.util.UUID
import scala.util.matching.Regex

/** Helpers for enhanced routing with path parameter extraction.
  *
  * These extractors work with SpiderApp's router which matches on (HttpMethod,
  * List[String]) patterns.
  *
  * Example usage:
  * {{{
  * import RoutingHelpers.*
  *
  * override val router = {
  *   case (GET, "users" :: IntParam(id) :: Nil) =>
  *     // id is an Int
  *   case (GET, "posts" :: UuidParam(uuid) :: Nil) =>
  *     // uuid is a UUID
  *   case (GET, "articles" :: LongParam(timestamp) :: Nil) =>
  *     // timestamp is a Long
  * }
  * }}}
  */
object RoutingHelpers {

  /** Extractor for integer path parameters.
    *
    * Example: /users/123 -> IntParam extracts 123 as Int
    */
  object IntParam {
    def unapply(s: String): Option[Int] = s.toIntOption
  }

  /** Extractor for long path parameters.
    *
    * Example: /posts/9876543210 -> LongParam extracts as Long
    */
  object LongParam {
    def unapply(s: String): Option[Long] = s.toLongOption
  }

  /** Extractor for UUID path parameters.
    *
    * Example: /resources/550e8400-e29b-41d4-a716-446655440000
    */
  object UuidParam {
    def unapply(s: String): Option[UUID] =
      try {
        Some(UUID.fromString(s))
      } catch {
        case _: IllegalArgumentException => None
      }
  }

  /** Extractor for double/decimal path parameters.
    *
    * Example: /prices/19.99 -> DoubleParam extracts 19.99
    */
  object DoubleParam {
    def unapply(s: String): Option[Double] = s.toDoubleOption
  }

  /** Extractor for boolean path parameters.
    *
    * Accepts: "true", "false" (case insensitive)
    */
  object BoolParam {
    def unapply(s: String): Option[Boolean] = s.toLowerCase match {
      case "true"  => Some(true)
      case "false" => Some(false)
      case _       => None
    }
  }

  /** Create a regex-based extractor for path segments.
    *
    * Example:
    * {{{
    * val EmailParam = RegexParam("""[\w.+-]+@[\w.-]+\.\w+""".r)
    * case (GET, "users" :: EmailParam(email) :: Nil) => ...
    * }}}
    */
  object RegexParam {
    def apply(pattern: Regex): RegexExtractor = new RegexExtractor(pattern)
  }

  class RegexExtractor(pattern: Regex) {
    def unapply(s: String): Option[String] =
      pattern.unapplySeq(s).flatMap(_.headOption)
  }

  /** Route builder helpers for common patterns.
    */
  object Routes {

    /** Helper to create a prefixed router.
      *
      * Example:
      * {{{
      * val apiRouter = Routes.withPrefix("api" :: "v1") {
      *   case (GET, "users" :: Nil) => userHandler
      *   case (GET, "posts" :: Nil) => postHandler
      * }
      * }}}
      */
    def withPrefix(
        prefix: List[String]
    )(routes: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]])
        : PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
      case (method, path) if path.startsWith(prefix) =>
        val remaining = path.drop(prefix.length)
        routes.apply((method, remaining))
    }

    /** Helper to create method-specific routers.
      */
    def get(
        pathMatcher: PartialFunction[List[String], RequestHandler[?, ?]]
    ): PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
      case (HttpMethod.GET, path) if pathMatcher.isDefinedAt(path) =>
        pathMatcher(path)
    }

    def post(
        pathMatcher: PartialFunction[List[String], RequestHandler[?, ?]]
    ): PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
      case (HttpMethod.POST, path) if pathMatcher.isDefinedAt(path) =>
        pathMatcher(path)
    }

    def put(
        pathMatcher: PartialFunction[List[String], RequestHandler[?, ?]]
    ): PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
      case (HttpMethod.PUT, path) if pathMatcher.isDefinedAt(path) =>
        pathMatcher(path)
    }

    def delete(
        pathMatcher: PartialFunction[List[String], RequestHandler[?, ?]]
    ): PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
      case (HttpMethod.DELETE, path) if pathMatcher.isDefinedAt(path) =>
        pathMatcher(path)
    }

    /** Combine multiple routers together.
      */
    def combine(
        routers: PartialFunction[
          (HttpMethod, List[String]),
          RequestHandler[?, ?]
        ]*
    ): PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
      routers.reduce(_ orElse _)
    }
  }

  /** Query parameter extraction helpers.
    */
  object QueryParams {

    /** Parse query string into a Map.
      *
      * Example: "name=John&age=30" -> Map("name" -> List("John"), "age" ->
      * List("30"))
      */
    def parse(queryString: String): Map[String, List[String]] = {
      if (queryString == null || queryString.isEmpty) {
        Map.empty
      } else {
        queryString
          .split('&')
          .flatMap { param =>
            param.split('=') match {
              case Array(key, value) =>
                Some(
                  java.net.URLDecoder.decode(key, "UTF-8") ->
                    java.net.URLDecoder.decode(value, "UTF-8")
                )
              case Array(key)        =>
                Some(java.net.URLDecoder.decode(key, "UTF-8") -> "")
              case _                 => None
            }
          }
          .groupBy(_._1)
          .map { case (k, v) => k -> v.map(_._2).toList }
      }
    }

    /** Get first value for a query parameter.
      */
    def get(params: Map[String, List[String]], key: String): Option[String] =
      params.get(key).flatMap(_.headOption)

    /** Get all values for a query parameter.
      */
    def getAll(params: Map[String, List[String]], key: String): List[String] =
      params.getOrElse(key, Nil)

    /** Get required parameter or return None if missing.
      */
    def required(
        params: Map[String, List[String]],
        key: String
    ): Option[String] =
      get(params, key).filter(_.nonEmpty)
  }
}
