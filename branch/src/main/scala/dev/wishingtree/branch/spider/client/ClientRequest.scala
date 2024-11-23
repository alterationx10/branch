package dev.wishingtree.branch.spider.client

import dev.wishingtree.branch.spider.ContentType
import java.net.URI
import java.net.http.HttpRequest

object ClientRequest {

  extension (sc: StringContext) {
    def uri(args: Any*): URI = URI.create(sc.s(args*))
  }

  extension (rb: HttpRequest.Builder) {
    def withContentType(contentType: ContentType): HttpRequest.Builder =
      rb.setHeader("Content-Type", contentType.content)
  }

  def builder(uri: URI): HttpRequest.Builder =
    HttpRequest.newBuilder(uri)

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
