package dev.wishingtree.branch.spider

case class ContentType(content: String)

object ContentType {

  extension (ct: ContentType) {
    def toHeader: (String, List[String]) = "Content-Type" -> List(ct.content)
  }

  // https://developer.mozilla.org/en-US/docs/Web/HTTP/MIME_types/Common_types

  val html       = ContentType("text/html")
  val plainText  = ContentType("text/plain")
  val javaScript = ContentType("text/javascript")

  val jpeg = ContentType("image/jpeg")
  val png  = ContentType("image/png")

  val octetStream = ContentType("application/octet-stream")
  val json        = ContentType("application/json")

}
