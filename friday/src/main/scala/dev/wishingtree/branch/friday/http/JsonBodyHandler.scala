package dev.wishingtree.branch.friday.http

import dev.wishingtree.branch.friday.JsonDecoder
import jdk.internal.net.http.ResponseSubscribers

import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandler

trait JsonBodyHandler[I] extends BodyHandler[I]{

}

object JsonBodyHandler {

  inline def of[I](using JsonDecoder[I]) = summon[JsonBodyHandler[I]]
  
  inline given derived[I](using decoder: JsonDecoder[I]):JsonBodyHandler[I] = {
    new JsonBodyHandler[I] {
      override def apply(responseInfo: HttpResponse.ResponseInfo): HttpResponse.BodySubscriber[I] =
        new ResponseSubscribers.ByteArraySubscriber[I](bytes =>
          decoder
            .decode(new String(bytes))
            .getOrElse(throw new Exception("Error mapping response"))
        )
    }
  }

}
