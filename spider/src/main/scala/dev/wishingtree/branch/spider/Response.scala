package dev.wishingtree.branch.spider

case class Response[A](
    body: A,
    headers: Map[String, List[String]] = Map(
      ContentType.octetStream.toHeader
    )
)

object Response {

  extension [A](r: Response[A]) {

    def withHeader(header: (String, String)) =
      r.copy(headers = r.headers + (header._1 -> List(header._2)))

    def withContentType(contentType: ContentType): Response[A] =
      r.copy(headers = r.headers + contentType.toHeader)

    def withContentType(contentType: String): Response[A] =
      withContentType(ContentType(contentType))

    def textContent: Response[A] =
      r.withContentType(ContentType.plainText)

  }

}
