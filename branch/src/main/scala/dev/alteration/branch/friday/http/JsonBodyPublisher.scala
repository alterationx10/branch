package dev.alteration.branch.friday.http

import dev.alteration.branch.friday.JsonEncoder
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers

object JsonBodyPublisher {

  /** Creates a HttpRequest.BodyPublisher for a given value using the provided
    * JsonEncoder to encode the value to a JSON string.
    */
  inline def of[I](
      i: I
  )(using jsonEncoder: JsonEncoder[I]): HttpRequest.BodyPublisher = {
    BodyPublishers.ofString(jsonEncoder.encode(i).toJsonString)
  }

}
