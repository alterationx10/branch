package dev.wishingtree.branch.spider.server

import dev.wishingtree.branch.spider.ContentType

/** A response model for an HTTP request.
  */
case class Response[A](
    body: A,
    headers: Map[String, List[String]] = Map(
      ContentType.bin.toHeader
    )
)

object Response {

  extension [A](r: Response[A]) {

    /** Adds a header to the response.
      */
    def withHeader(header: (String, String)) =
      r.copy(headers = r.headers + (header._1 -> List(header._2)))

    /** Adds a content type header to the response.
      */
    def withContentType(contentType: ContentType): Response[A] =
      r.copy(headers = r.headers + contentType.toHeader)

    /** Adds a content type header to the response.
      */
    def withContentType(contentType: String): Response[A] =
      withContentType(ContentType(contentType))

    /** Sets the response content type to [[ContentType.txt]].
      */
    def textContent: Response[A] =
      r.withContentType(ContentType.txt)

    /** Sets the response content type to [[ContentType.html]].
      */
    def htmlContent: Response[A] =
      r.withContentType(ContentType.html)

    /** Try to automatically set the contentType via [[ContentType.contentPF]]
      */
    def autoContent(ext: String): Response[A] = {
      val sanitized   = ext.split("\\.").toList.last
      val contentType = ContentType.contentPF(sanitized)
      r.withContentType(contentType)
    }
  }

}
