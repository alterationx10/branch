package dev.alteration.branch.spider.server

import scala.util.Try
import java.nio.charset.StandardCharsets
import java.io.FileInputStream
import java.nio.file.{Files, Path}

/** Parser for multipart/form-data request bodies (RFC 7578).
  *
  * Supports both fully-buffered parsing (for smaller uploads) and streaming
  * parsing (for large file uploads).
  */
object MultipartParser {

  /** Parse a fully-buffered multipart/form-data body.
    *
    * @param body
    *   The complete request body as a byte array
    * @param boundary
    *   The boundary string from the Content-Type header
    * @param config
    *   Parser configuration with size limits
    * @return
    *   Parsed multipart data or error
    */
  def parseMultipart(
      body: Array[Byte],
      boundary: String,
      config: BodyParser.ParserConfig
  ): Try[MultipartData] = Try {

    // Validate total size
    if (body.length > config.maxMultipartSize) {
      throw new IllegalArgumentException(
        s"Multipart body too large: ${body.length} bytes > ${config.maxMultipartSize} bytes"
      )
    }

    // Compute boundary markers
    val boundaryBytes      = s"\r\n--$boundary".getBytes(StandardCharsets.UTF_8)
    val startBoundaryBytes = s"--$boundary".getBytes(StandardCharsets.UTF_8)

    // Split body into parts
    val parts = splitByBoundary(body, boundaryBytes, startBoundaryBytes)

    // Parse each part
    var fields    = Map.empty[String, String]
    var files     = Map.empty[String, List[FileUpload]]
    var fileCount = 0

    for (part <- parts if part.nonEmpty) {
      try {
        parsePart(part) match {
          case TextPart(name, value) =>
            fields = fields + (name -> value)

          case FilePart(name, filename, contentType, data) =>
            // Check file count limit
            fileCount += 1
            if (fileCount > config.maxFileCount) {
              throw new IllegalArgumentException(
                s"Too many files: $fileCount > ${config.maxFileCount}"
              )
            }

            // Check file size limit
            if (data.length > config.maxFileSize) {
              throw new IllegalArgumentException(
                s"File '${filename.getOrElse("unknown")}' too large: ${data.length} bytes > ${config.maxFileSize} bytes"
              )
            }

            // Check allowed types
            config.allowedFileTypes.foreach { allowed =>
              if (
                !contentType
                  .exists(ct => allowed.exists(_.equalsIgnoreCase(ct)))
              ) {
                throw new IllegalArgumentException(
                  s"File type not allowed: ${contentType.getOrElse("unknown")}"
                )
              }
            }

            val upload =
              FileUpload(name, filename, contentType, data, data.length.toLong)
            files = files.updatedWith(name) {
              case Some(existing) => Some(existing :+ upload)
              case None           => Some(List(upload))
            }
        }
      } catch {
        case _: IllegalArgumentException
            if part.isEmpty || part
              .forall(b => b == '\r' || b == '\n' || b == ' ') =>
          // Skip empty parts silently
          ()
      }
    }

    MultipartData(fields, files)
  }

  /** Split body by boundary markers.
    *
    * Searches for boundary strings in the byte array and returns the parts
    * between them.
    */
  private def splitByBoundary(
      body: Array[Byte],
      boundary: Array[Byte],
      startBoundary: Array[Byte]
  ): List[Array[Byte]] = {
    val parts = scala.collection.mutable.ListBuffer.empty[Array[Byte]]

    // Find first boundary (may not have leading \r\n)
    var start = indexOfBytes(body, startBoundary, 0)
    if (start < 0) return List.empty

    start += startBoundary.length

    // Skip the line ending after the boundary
    if (
      start < body.length - 1 && body(start) == '\r' && body(start + 1) == '\n'
    ) {
      start += 2
    } else if (start < body.length && body(start) == '\n') {
      start += 1
    }

    // Find subsequent boundaries
    var pos = start
    while (pos < body.length) {
      val nextBoundaryPos = indexOfBytes(body, boundary, pos)
      if (nextBoundaryPos < 0) {
        // No more boundaries
        parts += body.slice(start, body.length)
        pos = body.length
      } else {
        // Found next boundary
        parts += body.slice(start, nextBoundaryPos)
        start = nextBoundaryPos + boundary.length

        // Skip line ending after boundary
        if (
          start < body.length - 1 && body(start) == '\r' && body(
            start + 1
          ) == '\n'
        ) {
          start += 2
        } else if (start < body.length && body(start) == '\n') {
          start += 1
        }

        // Check if this is the final boundary (ends with --)
        if (
          start < body.length - 1 && body(start) == '-' && body(
            start + 1
          ) == '-'
        ) {
          pos = body.length // Done
        } else {
          pos = start
        }
      }
    }

    parts.toList
  }

  /** Find the index of a byte pattern within a byte array.
    *
    * Uses a simple brute-force search. For better performance with large
    * bodies, could implement Boyer-Moore or KMP.
    */
  private def indexOfBytes(
      haystack: Array[Byte],
      needle: Array[Byte],
      fromIndex: Int
  ): Int = {
    if (needle.isEmpty) return fromIndex
    if (fromIndex + needle.length > haystack.length) return -1

    var i = fromIndex
    while (i <= haystack.length - needle.length) {
      var j     = 0
      var found = true
      while (j < needle.length && found) {
        if (haystack(i + j) != needle(j)) {
          found = false
        }
        j += 1
      }
      if (found) return i
      i += 1
    }
    -1
  }

  /** Parse a single multipart part (headers + body).
    *
    * Each part has headers separated from the body by \r\n\r\n.
    */
  private def parsePart(partBytes: Array[Byte]): Part = {
    // Handle empty parts (can happen between boundaries)
    if (
      partBytes.isEmpty || partBytes
        .forall(b => b == '\r' || b == '\n' || b == ' ')
    ) {
      // Return a dummy part that will be filtered out
      throw new IllegalArgumentException("Empty part")
    }

    // Find the headers/body separator (\r\n\r\n or \n\n)
    val separatorIndex = findHeaderBodySeparator(partBytes)

    if (separatorIndex < 0) {
      throw new IllegalArgumentException(
        "Invalid multipart part: no header/body separator found"
      )
    }

    // Split headers and body
    val headerBytes    = partBytes.slice(0, separatorIndex)
    val bodyStartIndex = {
      // Skip the separator
      if (
        separatorIndex + 3 < partBytes.length &&
        partBytes(separatorIndex) == '\r' &&
        partBytes(separatorIndex + 1) == '\n' &&
        partBytes(separatorIndex + 2) == '\r' &&
        partBytes(separatorIndex + 3) == '\n'
      ) {
        separatorIndex + 4
      } else if (
        separatorIndex + 1 < partBytes.length &&
        partBytes(separatorIndex) == '\n' &&
        partBytes(separatorIndex + 1) == '\n'
      ) {
        separatorIndex + 2
      } else {
        separatorIndex + 2 // Default \r\n
      }
    }
    val bodyBytes      = partBytes.slice(bodyStartIndex, partBytes.length)

    // Parse headers
    val headers = parsePartHeaders(headerBytes)

    // Extract Content-Disposition
    val contentDisposition = headers
      .get("content-disposition")
      .orElse(headers.get("Content-Disposition"))

    if (contentDisposition.isEmpty) {
      throw new IllegalArgumentException(
        "Missing Content-Disposition header in part"
      )
    }

    // Parse Content-Disposition parameters
    val (fieldName, filename) = parseContentDisposition(contentDisposition.get)

    // Get Content-Type (optional)
    val contentType = headers
      .get("content-type")
      .orElse(headers.get("Content-Type"))

    // Determine if this is a file or text field
    filename match {
      case Some(fname) =>
        FilePart(fieldName, Some(fname), contentType, bodyBytes)
      case None        =>
        val value = new String(bodyBytes, StandardCharsets.UTF_8)
        TextPart(fieldName, value)
    }
  }

  /** Find the index of the header/body separator in a part. */
  private def findHeaderBodySeparator(bytes: Array[Byte]): Int = {
    // Look for \r\n\r\n
    var i = 0
    while (i < bytes.length - 3) {
      if (
        bytes(i) == '\r' && bytes(i + 1) == '\n' &&
        bytes(i + 2) == '\r' && bytes(i + 3) == '\n'
      ) {
        return i
      }
      i += 1
    }

    // Fallback: look for \n\n
    i = 0
    while (i < bytes.length - 1) {
      if (bytes(i) == '\n' && bytes(i + 1) == '\n') {
        return i
      }
      i += 1
    }

    -1
  }

  /** Parse part headers into a map. */
  private def parsePartHeaders(
      headerBytes: Array[Byte]
  ): Map[String, String] = {
    val headersText = new String(headerBytes, StandardCharsets.UTF_8)
    val lines       = headersText.split("\r\n|\n")

    lines.flatMap { line =>
      val colonIdx = line.indexOf(':')
      if (colonIdx > 0) {
        val name  = line.substring(0, colonIdx).trim
        val value = line.substring(colonIdx + 1).trim
        Some(name -> value)
      } else {
        None
      }
    }.toMap
  }

  /** Parse Content-Disposition header to extract field name and filename.
    *
    * Example: Content-Disposition: form-data; name="photo"; filename="cat.jpg"
    */
  private def parseContentDisposition(
      value: String
  ): (String, Option[String]) = {
    val parts = value.split(";").map(_.trim)

    var fieldName: Option[String] = None
    var filename: Option[String]  = None

    for (part <- parts) {
      if (part.startsWith("name=")) {
        fieldName = Some(unquote(part.substring(5)))
      } else if (part.startsWith("filename=")) {
        filename = Some(unquote(part.substring(9)))
      } else if (part.startsWith("filename*=")) {
        // RFC 2231 extended format: filename*=UTF-8''filename.txt
        val extValue = part.substring(10)
        filename = Some(decodeRfc2231(extValue))
      }
    }

    fieldName match {
      case Some(name) => (name, filename)
      case None       =>
        throw new IllegalArgumentException(
          s"Missing 'name' parameter in Content-Disposition: $value"
        )
    }
  }

  /** Remove surrounding quotes from a string. */
  private def unquote(s: String): String = {
    if (s.startsWith("\"") && s.endsWith("\"") && s.length >= 2) {
      s.substring(1, s.length - 1)
    } else {
      s
    }
  }

  /** Decode RFC 2231 extended parameter format.
    *
    * Example: UTF-8''filename%20with%20spaces.txt
    */
  private def decodeRfc2231(value: String): String = {
    val parts = value.split("''", 2)
    if (parts.length == 2) {
      val charset = parts(0)
      val encoded = parts(1)
      // URL decode
      java.net.URLDecoder.decode(encoded, charset)
    } else {
      value
    }
  }

  /** Parse multipart data from a streaming request.
    *
    * This implementation saves files to temporary locations and returns
    * StreamingFileUpload objects that can read from those locations.
    *
    * @param stream
    *   The streaming request
    * @param boundary
    *   The boundary string from Content-Type header
    * @param config
    *   Parser configuration with size limits
    * @param tempDir
    *   Optional directory for temporary files (defaults to system temp)
    * @return
    *   Parsed multipart data with streaming file access
    */
  def parseMultipartStreaming(
      stream: StreamingRequest,
      boundary: String,
      config: BodyParser.ParserConfig,
      tempDir: Option[Path] = None
  ): Try[MultipartDataStreaming] = Try {

    val boundaryBytes      = s"\r\n--$boundary".getBytes(StandardCharsets.UTF_8)
    val startBoundaryBytes = s"--$boundary".getBytes(StandardCharsets.UTF_8)

    var fields         = Map.empty[String, String]
    var files          = Map.empty[String, List[StreamingFileUpload]]
    var fileCount      = 0
    var totalBytesRead = 0L

    // Use StreamReader for convenient reading
    stream.withReader { reader =>

      // State machine for parsing
      var state: ParseState = SeekingBoundary
      var buffer            = Array.empty[Byte]
      val chunkSize         = 8192

      while (totalBytesRead < config.maxMultipartSize) {
        val chunk = reader.read(chunkSize)
        if (chunk.isEmpty) {
          state = Done
        } else {
          buffer = buffer ++ chunk
          totalBytesRead += chunk.length

          state = state match {
            case SeekingBoundary =>
              // Look for start boundary
              val startIdx = indexOfBytes(buffer, startBoundaryBytes, 0)
              if (startIdx >= 0) {
                // Found boundary, move to reading headers
                buffer = buffer.drop(startIdx + startBoundaryBytes.length)
                // Skip line ending
                if (
                  buffer.length >= 2 && buffer(0) == '\r' && buffer(1) == '\n'
                ) {
                  buffer = buffer.drop(2)
                } else if (buffer.nonEmpty && buffer(0) == '\n') {
                  buffer = buffer.drop(1)
                }
                ReadingHeaders(Array.empty)
              } else {
                SeekingBoundary
              }

            case ReadingHeaders(headerAcc) =>
              // Look for header/body separator
              val sepIdx = findHeaderBodySeparator(buffer)
              if (sepIdx >= 0) {
                val headerBytes = headerAcc ++ buffer.slice(0, sepIdx)
                val headers     = parsePartHeaders(headerBytes)

                // Parse Content-Disposition
                val contentDisposition = headers
                  .get("content-disposition")
                  .orElse(headers.get("Content-Disposition"))
                  .getOrElse(
                    throw new IllegalArgumentException(
                      "Missing Content-Disposition"
                    )
                  )

                val (fieldName, filename) =
                  parseContentDisposition(contentDisposition)
                val contentType           = headers
                  .get("content-type")
                  .orElse(headers.get("Content-Type"))

                // Skip separator
                val bodyStart =
                  if (
                    sepIdx + 3 < buffer.length &&
                    buffer(sepIdx) == '\r' && buffer(sepIdx + 1) == '\n' &&
                    buffer(sepIdx + 2) == '\r' && buffer(sepIdx + 3) == '\n'
                  ) {
                    sepIdx + 4
                  } else {
                    sepIdx + 2
                  }

                buffer = buffer.drop(bodyStart)
                ReadingBody(fieldName, filename, contentType, Array.empty)
              } else {
                // Need more data
                ReadingHeaders(headerAcc ++ buffer)
              }

            case ReadingBody(fieldName, filename, contentType, bodyAcc) =>
              // Look for next boundary
              val boundaryIdx = indexOfBytes(buffer, boundaryBytes, 0)
              if (boundaryIdx >= 0) {
                // Found boundary - part is complete
                val bodyBytes = bodyAcc ++ buffer.slice(0, boundaryIdx)

                filename match {
                  case Some(fname) =>
                    // File upload
                    fileCount += 1
                    if (fileCount > config.maxFileCount) {
                      throw new IllegalArgumentException(
                        s"Too many files: $fileCount > ${config.maxFileCount}"
                      )
                    }
                    if (bodyBytes.length > config.maxFileSize) {
                      throw new IllegalArgumentException(
                        s"File '$fname' too large: ${bodyBytes.length} > ${config.maxFileSize}"
                      )
                    }

                    config.allowedFileTypes.foreach { allowed =>
                      if (
                        !contentType
                          .exists(ct => allowed.exists(_.equalsIgnoreCase(ct)))
                      ) {
                        throw new IllegalArgumentException(
                          s"File type not allowed: ${contentType.getOrElse("unknown")}"
                        )
                      }
                    }

                    // Save to temp file
                    val tempFile = tempDir match {
                      case Some(dir) =>
                        Files.createTempFile(dir, "upload-", ".tmp")
                      case None      => Files.createTempFile("upload-", ".tmp")
                    }
                    Files.write(tempFile, bodyBytes)

                    val upload = StreamingFileUpload(
                      fieldName,
                      Some(fname),
                      contentType,
                      () => new FileInputStream(tempFile.toFile),
                      Some(bodyBytes.length.toLong)
                    )

                    files = files.updatedWith(fieldName) {
                      case Some(existing) => Some(existing :+ upload)
                      case None           => Some(List(upload))
                    }

                  case None =>
                    // Text field
                    val value = new String(bodyBytes, StandardCharsets.UTF_8)
                    fields = fields + (fieldName -> value)
                }

                // Move past boundary
                buffer = buffer.drop(boundaryIdx + boundaryBytes.length)

                // Check for end marker (--)
                if (
                  buffer.length >= 2 && buffer(0) == '-' && buffer(1) == '-'
                ) {
                  Done
                } else {
                  // Skip line ending and continue
                  if (
                    buffer.length >= 2 && buffer(0) == '\r' && buffer(1) == '\n'
                  ) {
                    buffer = buffer.drop(2)
                  } else if (buffer.nonEmpty && buffer(0) == '\n') {
                    buffer = buffer.drop(1)
                  }
                  ReadingHeaders(Array.empty)
                }
              } else {
                // Keep accumulating body
                ReadingBody(fieldName, filename, contentType, bodyAcc ++ buffer)
              }

            case Done =>
              Done
          }
        }

        if (state == Done) {
          // Exit early
          totalBytesRead = config.maxMultipartSize
        }
      }
    }

    MultipartDataStreaming(fields, files)
  }

  // State machine states for streaming parser
  private sealed trait ParseState
  private case object SeekingBoundary                         extends ParseState
  private case class ReadingHeaders(accumulated: Array[Byte]) extends ParseState
  private case class ReadingBody(
      fieldName: String,
      filename: Option[String],
      contentType: Option[String],
      accumulated: Array[Byte]
  ) extends ParseState
  private case object Done                                    extends ParseState

  // Internal representation of a parsed part
  private sealed trait Part
  private case class TextPart(name: String, value: String) extends Part
  private case class FilePart(
      name: String,
      filename: Option[String],
      contentType: Option[String],
      data: Array[Byte]
  ) extends Part
}
