package dev.wishingtree.branch.friday.http

import dev.wishingtree.branch.friday.JsonDecoder

import java.net.http.HttpResponse
import java.net.http.HttpResponse.{BodyHandler, BodySubscribers}
import java.nio.charset.Charset
import scala.util.Try

/** A type-class for a BodyHandler used for decoding JSON responses
  */
trait JsonBodyHandler[I] extends BodyHandler[I] {}

object JsonBodyHandler {

  /** Creates a BodyHandler for a given type using the provided JsonDecoder
    */
  inline def of[I](using JsonDecoder[I]): JsonBodyHandler[Try[I]] =
    summon[JsonBodyHandler[Try[I]]]

  /** Derives a BodyHandler for a given type using the provided JsonDecoder
    */
  protected class DerivedJsonBodyHandler[I](using decoder: JsonDecoder[I])
      extends JsonBodyHandler[Try[I]] {
    override def apply(
        responseInfo: HttpResponse.ResponseInfo
    ): HttpResponse.BodySubscriber[Try[I]] = {
      BodySubscribers.mapping(
        BodySubscribers.ofString(Charset.defaultCharset()),
        str => decoder.decode(str)
      )
    }
  }

  inline given derived[I](using
      decoder: JsonDecoder[I]
  ): JsonBodyHandler[Try[I]] =
    new DerivedJsonBodyHandler[I]

}
