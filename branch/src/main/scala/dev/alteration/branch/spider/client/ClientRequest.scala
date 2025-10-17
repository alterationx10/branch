package dev.alteration.branch.spider.client

import dev.alteration.branch.spider.common.ContentType
import java.net.URI
import java.net.http.HttpRequest

/** A helper object with utilities for creating and manipulating HttpRequest
  * objects.
  */
object ClientRequest {

  extension (sc: StringContext) {

    /** A string interpolator to create a URI from a string.
      */
    def uri(args: Any*): URI = URI.create(sc.s(args*))
  }

  extension (rb: HttpRequest.Builder) {

    /** A method to set the content type of the request.
      */
    def withContentType(contentType: ContentType): HttpRequest.Builder =
      rb.setHeader("Content-Type", contentType.content)
  }

  /** A default builder for creating a new HttpRequest targeting the given uri.
    */
  def builder(uri: URI): HttpRequest.Builder =
    HttpRequest.newBuilder(uri)

  /** A method to build a new HttpRequest with the given settings.
    */
  def build(
      uri: URI,
      settings: HttpRequest.Builder => HttpRequest.Builder*
  ): HttpRequest = {
    settings
      .foldLeft(
        builder(uri)
      )((b, s) => s(b))
      .build()
  }

}
