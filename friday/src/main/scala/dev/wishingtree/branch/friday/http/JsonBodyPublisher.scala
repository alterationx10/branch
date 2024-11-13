package dev.wishingtree.branch.friday.http

import dev.wishingtree.branch.friday.JsonEncoder

import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers

object JsonBodyPublisher {

  inline def of[I](i: I)(using jsonEncoder: JsonEncoder[I]): HttpRequest.BodyPublisher = {
    BodyPublishers.ofString(jsonEncoder.encode(i).toJsonString)
  }
  
}
