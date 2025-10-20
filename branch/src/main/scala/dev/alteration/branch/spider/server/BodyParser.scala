package dev.alteration.branch.spider.server

import dev.alteration.branch.friday.JsonDecoder

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import scala.util.{Failure, Success, Try}

/** Utilities for parsing HTTP request bodies.
  */
object BodyParser {

  /** Result of parsing a request body. */
  sealed trait ParseResult[+A]
  case class ParseSuccess[A](value: A)   extends ParseResult[A]
  case class ParseFailure(error: String) extends ParseResult[Nothing]
  case object UnsupportedContentType     extends ParseResult[Nothing]
  case object BodyTooLarge               extends ParseResult[Nothing]

  /** Configuration for body parsing. */
  case class ParserConfig(
      maxJsonSize: Long = 10 * 1024 * 1024, // 10MB
      maxFormSize: Long = 1 * 1024 * 1024,  // 1MB
      maxTextSize: Long = 5 * 1024 * 1024   // 5MB
  )

  object ParserConfig {
    val default: ParserConfig = ParserConfig()

    val strict: ParserConfig = ParserConfig(
      maxJsonSize = 1 * 1024 * 1024, // 1MB
      maxFormSize = 512 * 1024,      // 512KB
      maxTextSize = 1 * 1024 * 1024  // 1MB
    )

    val permissive: ParserConfig = ParserConfig(
      maxJsonSize = 50 * 1024 * 1024, // 50MB
      maxFormSize = 10 * 1024 * 1024, // 10MB
      maxTextSize = 25 * 1024 * 1024  // 25MB
    )
  }

  /** Parse application/x-www-form-urlencoded body.
    *
    * @param body
    *   The request body as a string or bytes
    * @return
    *   Map of form field names to values
    */
  def parseFormUrlEncoded(body: String): Map[String, String] = {
    if (body.isEmpty) {
      Map.empty
    } else {
      body
        .split("&")
        .flatMap { pair =>
          pair.split("=", 2) match {
            case Array(key)        =>
              Some(
                URLDecoder.decode(key, StandardCharsets.UTF_8) -> ""
              )
            case Array(key, value) =>
              Some(
                URLDecoder.decode(key, StandardCharsets.UTF_8) ->
                  URLDecoder.decode(value, StandardCharsets.UTF_8)
              )
            case _                 => None
          }
        }
        .toMap
    }
  }

  /** Parse application/x-www-form-urlencoded body from bytes.
    *
    * @param bytes
    *   The request body as bytes
    * @return
    *   Map of form field names to values
    */
  def parseFormUrlEncoded(bytes: Array[Byte]): Map[String, String] = {
    val body = new String(bytes, StandardCharsets.UTF_8)
    parseFormUrlEncoded(body)
  }

  /** Parse JSON body using a JsonDecoder.
    *
    * @param body
    *   The request body as a string or bytes
    * @param decoder
    *   The JsonDecoder for type A
    * @return
    *   Parsed value or error
    */
  def parseJson[A](body: String)(using
      decoder: JsonDecoder[A]
  ): Try[A] = {
    decoder.decode(body)
  }

  /** Parse JSON body from bytes.
    *
    * @param bytes
    *   The request body as bytes
    * @param decoder
    *   The JsonDecoder for type A
    * @return
    *   Parsed value or error
    */
  def parseJson[A](bytes: Array[Byte])(using
      decoder: JsonDecoder[A]
  ): Try[A] = {
    val body = new String(bytes, StandardCharsets.UTF_8)
    parseJson(body)
  }

  /** Extension methods for Request to parse bodies. */
  extension [A](request: Request[A]) {

    /** Get the Content-Type header value. */
    def contentType: Option[String] = {
      request.headers
        .get("Content-Type")
        .orElse(request.headers.get("content-type"))
        .flatMap(_.headOption)
        .map(_.split(";")(0).trim.toLowerCase) // Remove charset, etc.
    }

    /** Check if the body size exceeds a limit.
      *
      * Note: This checks the type A's byte representation if it's Array[Byte],
      * otherwise it's an approximation based on string representation.
      */
    def bodySizeBytes: Long = {
      request.body match {
        case bytes: Array[Byte] => bytes.length.toLong
        case str: String        => str.getBytes(StandardCharsets.UTF_8).length.toLong
        case _                  => 0L
      }
    }

    /** Check if body size is within limit. */
    def isBodyWithinLimit(maxSize: Long): Boolean = {
      bodySizeBytes <= maxSize
    }
  }

  extension (request: Request[Array[Byte]]) {

    /** Parse form-urlencoded body. */
    def parseFormBody: Map[String, String] = {
      parseFormUrlEncoded(request.body)
    }

    /** Parse JSON body. */
    def parseJsonBody[A](using decoder: JsonDecoder[A]): Try[A] = {
      parseJson[A](request.body)
    }

    /** Parse body based on Content-Type with size limits.
      *
      * @param config
      *   Parser configuration with size limits
      * @return
      *   ParseResult with the parsed data or error
      */
    def parseBodyAuto[A](
        config: ParserConfig = ParserConfig.default
    )(using
        decoder: JsonDecoder[A]
    ): ParseResult[Either[Map[String, String], A]] = {
      request.contentType match {
        case Some("application/json") =>
          // Check size limit
          if (!request.isBodyWithinLimit(config.maxJsonSize)) {
            BodyTooLarge
          } else {
            parseJson[A](request.body) match {
              case Success(value) => ParseSuccess(Right(value))
              case Failure(e)     => ParseFailure(e.getMessage)
            }
          }

        case Some("application/x-www-form-urlencoded") =>
          // Check size limit
          if (!request.isBodyWithinLimit(config.maxFormSize)) {
            BodyTooLarge
          } else {
            Try(parseFormUrlEncoded(request.body)) match {
              case Success(form) => ParseSuccess(Left(form))
              case Failure(e)    => ParseFailure(e.getMessage)
            }
          }

        case _ => UnsupportedContentType
      }
    }

    /** Parse body as form data with size limit.
      *
      * @param config
      *   Parser configuration
      * @return
      *   ParseResult with the form data or error
      */
    def parseFormBodySafe(
        config: ParserConfig = ParserConfig.default
    ): ParseResult[Map[String, String]] = {
      if (!request.isBodyWithinLimit(config.maxFormSize)) {
        BodyTooLarge
      } else {
        Try(parseFormUrlEncoded(request.body)) match {
          case Success(form) => ParseSuccess(form)
          case Failure(e)    => ParseFailure(e.getMessage)
        }
      }
    }

    /** Parse body as JSON with size limit.
      *
      * @param config
      *   Parser configuration
      * @return
      *   ParseResult with the parsed data or error
      */
    def parseJsonBodySafe[A](
        config: ParserConfig = ParserConfig.default
    )(using
        decoder: JsonDecoder[A]
    ): ParseResult[A] = {
      if (!request.isBodyWithinLimit(config.maxJsonSize)) {
        BodyTooLarge
      } else {
        parseJson[A](request.body) match {
          case Success(value) => ParseSuccess(value)
          case Failure(e)     => ParseFailure(e.getMessage)
        }
      }
    }
  }

  extension (request: Request[String]) {

    /** Parse form-urlencoded body from String body. */
    @scala.annotation.targetName("parseFormBodyString")
    def parseFormBody: Map[String, String] = {
      parseFormUrlEncoded(request.body)
    }

    /** Parse JSON body from String body. */
    @scala.annotation.targetName("parseJsonBodyString")
    def parseJsonBody[A](using decoder: JsonDecoder[A]): Try[A] = {
      parseJson[A](request.body)
    }
  }

  /** Helper to create a 400 Bad Request response for parse failures. */
  def badRequestResponse(error: String): Response[String] = {
    Response(400, error)
  }

  /** Helper to create a 413 Payload Too Large response. */
  def payloadTooLargeResponse: Response[String] = {
    Response(413, "Request body too large")
  }

  /** Helper to create a 415 Unsupported Media Type response. */
  def unsupportedMediaTypeResponse: Response[String] = {
    Response(415, "Unsupported Content-Type")
  }
}
