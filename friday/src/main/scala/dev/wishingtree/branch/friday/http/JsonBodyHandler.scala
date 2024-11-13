package dev.wishingtree.branch.friday.http

import dev.wishingtree.branch.friday.JsonDecoder

import java.net.http.HttpResponse
import java.net.http.HttpResponse.{BodyHandler, BodySubscribers}
import java.nio.charset.Charset
import scala.util.Try

trait JsonBodyHandler[I] extends BodyHandler[I] {}

object JsonBodyHandler {

  inline def of[I](using JsonDecoder[I]): JsonBodyHandler[Try[I]] =
    summon[JsonBodyHandler[Try[I]]]

  inline given derived[I](using
      decoder: JsonDecoder[I]
  ): JsonBodyHandler[Try[I]] = {
    new JsonBodyHandler[Try[I]] {
      override def apply(
          responseInfo: HttpResponse.ResponseInfo
      ): HttpResponse.BodySubscriber[Try[I]] = {
        BodySubscribers.mapping(
          BodySubscribers.ofString(Charset.defaultCharset()),
          str => decoder.decode(str)
        )
      }
    }
  }

}
