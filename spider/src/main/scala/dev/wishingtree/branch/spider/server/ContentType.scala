package dev.wishingtree.branch.spider.server

import dev.wishingtree.branch.spider.server.ContentType

case class ContentType(content: String)

object ContentType {

  extension (ct: ContentType) {
    def toHeader: (String, List[String]) = "Content-Type" -> List(ct.content)
  }

  // https://developer.mozilla.org/en-US/docs/Web/HTTP/MIME_types/Common_types

  val aac        = ContentType("audio/aac")
  val abw        = ContentType("application/x-abiword")
  val apng       = ContentType("image/apng")
  val arc        = ContentType("application/x-freearc")
  val avif       = ContentType("image/avif")
  val avi        = ContentType("video/x-msvideo")
  val azw        = ContentType("application/vnd.amazon.ebook")
  val bin        = ContentType("application/octet-stream")
  val bmp        = ContentType("image/bmp")
  val bz         = ContentType("application/x-bzip")
  val bz2        = ContentType("application/x-bzip2")
  val cda        = ContentType("application/x-cdf")
  val csh        = ContentType("application/x-csh")
  val css        = ContentType("text/css")
  val csv        = ContentType("text/csv")
  val doc        = ContentType("application/msword")
  val docx       = ContentType(
    " application/vnd.openxmlformats-officedocument.wordprocessingml.document "
  )
  val eot        = ContentType("application/vnd.ms-fontobject")
  val epub       = ContentType("application/epub+zip")
  val gz         = ContentType("application/gzip")
  val xgz        = ContentType("application/x-gzip")
  val gif        = ContentType("image/gif")
  val html       = ContentType("text/html")    // .html /html
  val ico        = ContentType("image/vnd.microsoft.icon")
  val ics        = ContentType("text/calendar")
  val jar        = ContentType("application/java-archive")
  val jpeg       = ContentType("image/jpeg")   // .jpg .jpeg
  val js         = ContentType("test/javascript")
  val json       = ContentType("application/json")
  val jsonld     = ContentType("application/ld+json")
  val midi       = ContentType("audio/midi")   // .mid .midi
  val xmidi      = ContentType("audio/x-midi") // .mid .midi
  val mjs        = ContentType("text/javascript")
  val mp3        = ContentType("audio/mpeg")
  val mp4        = ContentType("video/mp4")
  val mpeg       = ContentType("video/mpeg")
  val mpkg       = ContentType("application/vnd.apple.installer+xml")
  val odp        = ContentType("application/vnd.oasis.opendocument.presentation")
  val ods        = ContentType("application/vnd.oasis.opendocument.spreadsheet")
  val odt        = ContentType("application/vnd.oasis.opendocument.text")
  val oga        = ContentType("audio/ogg")
  val ogv        = ContentType("video/ogg")
  val ogx        = ContentType("application/ogg")
  val opus       = ContentType("audio/ogg")
  val otf        = ContentType("font/otf")
  val png        = ContentType("image/png")
  val pdf        = ContentType("application/pdf")
  val php        = ContentType("application/x-httpd-php")
  val ppt        = ContentType("application/vnd.ms-powerpoint")
  val pptx       = ContentType(
    " application/vnd.openxmlformats-officedocument.presentationml.presentation "
  )
  val rar        = ContentType("application/vnd.rar")
  val rtf        = ContentType("application/rtf")
  val sh         = ContentType("application/x-sh")
  val svg        = ContentType("image/svg+xml")
  val tar        = ContentType("application/x-tar")
  val tiff       = ContentType("image/tiff")   // .tif
  val ts         = ContentType("video/mp2t")
  val ttf        = ContentType("font/ttf")
  val txt        = ContentType("text/plain")
  val vsd        = ContentType("application/vnd.visio")
  val wav        = ContentType("audio/wav")
  val weba       = ContentType("audio/webm")
  val webm       = ContentType("video/webm")
  val webp       = ContentType("image/webp")
  val woff       = ContentType("font/woff")
  val woff2      = ContentType("font/woff2")
  val xhtml      = ContentType("application/xhtml+xml")
  val xls        = ContentType("application/vnd.ms-excel")
  val xlsx       = ContentType(
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  )
  val xml        = ContentType("application/xml")
  val xul        = ContentType("application/vnd.mozilla.xul+xml")
  val zip        = ContentType("application/zip")
  val xzip       = ContentType("application/x-zip-compressed")
  val `3gp`      = ContentType("video/3gpp")
  val `3gpAudio` = ContentType("audio/3gpp")
  val `3g2`      = ContentType("video/3gpp2")
  val `3g2Audio` = ContentType("audio/3gpp2")
  val `7z`       = ContentType("application/x-7z-compressed")

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
