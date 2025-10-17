package dev.alteration.branch.spider.common

/** A helper class for Content-Type headers
  * @param content
  */
case class ContentType(content: String)

object ContentType {

  extension (ct: ContentType) {

    /** Convert a ContentType to a header
      * @return
      */
    def toHeader: (String, List[String]) = "Content-Type" -> List(ct.content)
  }

  /** "audio/aac"
    */
  val aac = ContentType("audio/aac")

  /** "application/x-abiword"
    */
  val abw = ContentType("application/x-abiword")

  /** "image/apng"
    */
  val apng = ContentType("image/apng")

  /** "application/x-freearc"
    */
  val arc = ContentType("application/x-freearc")

  /** "image/avif"
    */
  val avif = ContentType("image/avif")

  /** "video/x-msvideo"
    */
  val avi = ContentType("video/x-msvideo")

  /** "application/vnd.amazon.ebook"
    */
  val azw = ContentType("application/vnd.amazon.ebook")

  /** "application/octet-stream"
    */
  val bin = ContentType("application/octet-stream")

  /** "image/bmp"
    */
  val bmp = ContentType("image/bmp")

  /** "application/x-bzip"
    */
  val bz = ContentType("application/x-bzip")

  /** "application/x-bzip2"
    */
  val bz2 = ContentType("application/x-bzip2")

  /** "application/x-cdf"
    */
  val cda = ContentType("application/x-cdf")

  /** "application/x-csh"
    */
  val csh = ContentType("application/x-csh")

  /** "text/css"
    */
  val css = ContentType("text/css")

  /** "text/csv"
    */
  val csv = ContentType("text/csv")

  /** "application/msword"
    */
  val doc = ContentType("application/msword")

  /** "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    */
  val docx = ContentType(
    " application/vnd.openxmlformats-officedocument.wordprocessingml.document "
  )

  /** "application/vnd.ms-fontobject"
    */
  val eot = ContentType("application/vnd.ms-fontobject")

  /** "application/epub+zip"
    */
  val epub = ContentType("application/epub+zip")

  /** "application/gzip"
    */
  val gz = ContentType("application/gzip")

  /** "image/x-gzip"
    */
  val xgz = ContentType("application/x-gzip")

  /** "image/gif"
    */
  val gif = ContentType("image/gif")

  /** "text/html"
    */
  val html = ContentType("text/html") // .html /html

  /** "image/vnd.microsoft.icon"
    */
  val ico = ContentType("image/vnd.microsoft.icon")

  /** "text/calendar"
    */
  val ics = ContentType("text/calendar")

  /** "application/java-archive"
    */
  val jar = ContentType("application/java-archive")

  /** "image/jpeg" .jpg, .jpeg
    */
  val jpeg = ContentType("image/jpeg")

  /** "text/javascript"
    */
  val js = ContentType("text/javascript")

  /** "application/json"
    */
  val json = ContentType("application/json")

  /** "application/ld+json"
    */
  val jsonld = ContentType("application/ld+json")

  /** "audio/midi" .mid, .midi
    */
  val midi = ContentType("audio/midi")

  /** "audio/x-midi" .mid, .midi
    */
  val xmidi = ContentType("audio/x-midi")

  /** "text/javascript"
    */
  val mjs = ContentType("text/javascript")

  /** "audio/mpeg"
    */
  val mp3 = ContentType("audio/mpeg")

  /** "video/mp4"
    */
  val mp4 = ContentType("video/mp4")

  /** "video/mpeg"
    */
  val mpeg = ContentType("video/mpeg")

  /** "application/vnd.apple.installer+xml"
    */
  val mpkg = ContentType("application/vnd.apple.installer+xml")

  /** "application/vnd.oasis.opendocument.presentation"
    */
  val odp = ContentType("application/vnd.oasis.opendocument.presentation")

  /** "application/vnd.oasis.opendocument.spreadsheet"
    */
  val ods = ContentType("application/vnd.oasis.opendocument.spreadsheet")

  /** "application/vnd.oasis.opendocument.text"
    */
  val odt = ContentType("application/vnd.oasis.opendocument.text")

  /** "audio/ogg"
    */
  val oga = ContentType("audio/ogg")

  /** "video/ogg"
    */
  val ogv = ContentType("video/ogg")

  /** "application/ogg"
    */
  val ogx = ContentType("application/ogg")

  /** "audio/ogg"
    */
  val opus = ContentType("audio/ogg")

  /** "font/otf"
    */
  val otf = ContentType("font/otf")

  /** "image/png"
    */
  val png = ContentType("image/png")

  /** "application/pdf"
    */
  val pdf = ContentType("application/pdf")

  /** "application/x-httpd-php"
    */
  val php = ContentType("application/x-httpd-php")

  /** "application/vnd.ms-powerpoint"
    */
  val ppt = ContentType("application/vnd.ms-powerpoint")

  /** "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    */
  val pptx = ContentType(
    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
  )

  /** "application/vnd.rar"
    */
  val rar = ContentType("application/vnd.rar")

  /** "application/rtf"
    */
  val rtf = ContentType("application/rtf")

  /** "application/x-sh"
    */
  val sh = ContentType("application/x-sh")

  /** "image/svg+xml"
    */
  val svg = ContentType("image/svg+xml")

  /** "application/x-tar"
    */
  val tar = ContentType("application/x-tar")

  /** "image/tiff" .tif
    */
  val tiff = ContentType("image/tiff")

  /** "video/mp2t"
    */
  val ts = ContentType("video/mp2t")

  /** "font/ttf"
    */
  val ttf = ContentType("font/ttf")

  /** "text/plain"
    */
  val txt = ContentType("text/plain")

  /** "application/vnd.visio"
    */
  val vsd = ContentType("application/vnd.visio")

  /** "audio/wav"
    */
  val wav = ContentType("audio/wav")

  /** "audio/webm"
    */
  val weba = ContentType("audio/webm")

  /** "video/webm"
    */
  val webm = ContentType("video/webm")

  /** "image/webp"
    */
  val webp = ContentType("image/webp")

  /** "font/woff"
    */
  val woff = ContentType("font/woff")

  /** "font/woff2"
    */
  val woff2 = ContentType("font/woff2")

  /** "application/xhtml+xml"
    */
  val xhtml = ContentType("application/xhtml+xml")

  /** "application/vnd.ms-excel"
    */
  val xls = ContentType("application/vnd.ms-excel")

  /** "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    */
  val xlsx = ContentType(
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  )

  /** "application/xml"
    */
  val xml = ContentType("application/xml")

  /** "application/vnd.mozilla.xul+xml"
    */
  val xul = ContentType("application/vnd.mozilla.xul+xml")

  /** "application/zip"
    */
  val zip = ContentType("application/zip")

  /** "application/x-zip-compressed"
    */
  val xzip = ContentType("application/x-zip-compressed")

  /** "video/3gpp"
    */
  val `3gp` = ContentType("video/3gpp")

  /** "audio/3gpp"
    */
  val `3gpAudio` = ContentType("audio/3gpp")

  /** "video/3gpp2"
    */
  val `3g2` = ContentType("video/3gpp2")

  /** "audio/3gpp2"
    */
  val `3g2Audio` = ContentType("audio/3gpp2")

  /** "application/x-7z-compressed"
    */
  val `7z` = ContentType("application/x-7z-compressed")

  /** A partial function to convert file extensions to content types. Defaults
    * to [[bin]], so will always return a [[ContentType]].
    */
  val contentPF: PartialFunction[String, ContentType] = {
    case "aac"          => aac
    case "abw"          => abw
    case "apng"         => apng
    case "arc"          => arc
    case "avif"         => avif
    case "avi"          => avi
    case "azw"          => azw
    case "bin"          => bin
    case "bmp"          => bmp
    case "bz"           => bz
    case "bz2"          => bz2
    case "cda"          => cda
    case "csh"          => csh
    case "css"          => css
    case "csv"          => csv
    case "doc"          => doc
    case "docx"         => docx
    case "eot"          => eot
    case "epub"         => epub
    case "gz"           => gz
    case "gif"          => gif
    case "htm" | "html" => html
    case "ico"          => ico
    case "ics"          => ics
    case "jar"          => jar
    case "jpeg" | "jpg" => jpeg
    case "js"           => js
    case "json"         => json
    case "jsonld"       => jsonld
    case "mid" | "midi" => midi
    case "mjs"          => mjs
    case "mp3"          => mp3
    case "mp4"          => mp4
    case "mpeg"         => mpeg
    case "mpkg"         => mpkg
    case "odp"          => odp
    case "ods"          => ods
    case "odt"          => odt
    case "oga"          => oga
    case "ogv"          => ogv
    case "ogx"          => ogx
    case "opus"         => opus
    case "otf"          => otf
    case "png"          => png
    case "pdf"          => pdf
    case "php"          => php
    case "ppt"          => ppt
    case "pptx"         => pptx
    case "rar"          => rar
    case "rtf"          => rtf
    case "sh"           => sh
    case "svg"          => svg
    case "tar"          => tar
    case "tif" | "tiff" => tiff
    case "ts"           => ts
    case "ttf"          => ttf
    case "txt"          => txt
    case "vsd"          => vsd
    case "wav"          => wav
    case "weba"         => weba
    case "webm"         => webm
    case "webp"         => webp
    case "woff"         => woff
    case "woff2"        => woff2
    case "xhtml"        => xhtml
    case "xls"          => xls
    case "xlsx"         => xlsx
    case "xml"          => xml
    case "xul"          => xul
    case "zip"          => zip
    case "3gp"          => `3gp`
    case "3g2"          => `3g2`
    case "7z"           => `7z`
    case _              => bin
  }

}
