package dev.alteration.branch.friday.http

import dev.alteration.branch.friday.{JsonDecoder, JsonEncoder}
import dev.alteration.branch.spider.server.Response
import dev.alteration.branch.spider.common.ContentType

/** A wrapper for JSON-encoded data as bytes.
  *
  * This type preserves the original type information while holding the
  * serialized JSON bytes, making APIs more self-documenting.
  *
  * @param bytes The JSON-encoded bytes
  * @tparam A The type that was serialized to JSON
  */
case class JsonBody[A](bytes: Array[Byte])

object JsonBody {

  /** Conversion from JsonBody to Array[Byte] for writing to the wire.
    */
  given [A]: Conversion[JsonBody[A], Array[Byte]] = _.bytes
}

object JsonConversions {

  /** A Conversion[Array[Byte], A], useful for marshalling request bodies in
    * spider
    */
  def convertFromBytes[A](using
      decoder: JsonDecoder[A]
  ): Conversion[Array[Byte], A] =
    (bytes: Array[Byte]) =>
      decoder
        .decode(new String(bytes))
        .getOrElse(throw new IllegalArgumentException("Failed to decode"))

  /** A Conversion[A, Array[Byte]], useful for marshalling response bodies in
    * spider
    */
  def convertToBytes[A](
      removeNulls: Boolean = false
  )(using encoder: JsonEncoder[A]): Conversion[A, Array[Byte]] = { (a: A) =>
    val json =
      if removeNulls then encoder.encode(a).removeNulls()
      else encoder.encode(a)
    json.toJsonString.getBytes
  }

  /** Extension methods for Response to support JSON serialization.
    */
  extension [A](response: Response[A]) {

    /** Serialize the response body to JSON bytes.
      *
      * This is a convenience method for the common case of returning JSON
      * responses. It uses the JsonEncoder typeclass to serialize the body and
      * sets the Content-Type header to application/json.
      *
      * The returned type `JsonBody[A]` preserves the original type information
      * while holding the serialized bytes, making APIs more self-documenting.
      *
      * Example:
      * {{{
      * import dev.alteration.branch.friday.http.JsonConversions.*
      *
      * case class User(name: String, age: Int)
      *
      * // Handler signature is now more informative:
      * // RequestHandler[Unit, JsonBody[User]]
      * Response(200, User("Alice", 30)).jsonBody()
      * }}}
      *
      * @param encoder
      *   The JsonEncoder for type A
      * @param removeNulls
      *   If true, remove null fields from the JSON output
      * @return
      *   A Response[JsonBody[A]] with JSON content
      */
    def jsonBody(
        removeNulls: Boolean = false
    )(using encoder: JsonEncoder[A]): Response[JsonBody[A]] = {
      val jsonBytes = convertToBytes[A](removeNulls = removeNulls)(using
        encoder
      )(response.body)
      Response(
        response.statusCode,
        JsonBody[A](jsonBytes),
        response.headers + ContentType.json.toHeader
      )
    }
  }

}
