package dev.alteration.branch.spider.server

import java.io.InputStream

/** Represents a parsed multipart/form-data request body.
  *
  * Contains both text form fields and uploaded files. Files can have multiple
  * uploads for the same field name (e.g., multiple file inputs with the same
  * name attribute).
  *
  * @param fields
  *   Map of field names to their text values
  * @param files
  *   Map of field names to lists of uploaded files
  */
case class MultipartData(
    fields: Map[String, String],
    files: Map[String, List[FileUpload]]
) {

  /** Get all files as a flat list. */
  def allFiles: List[FileUpload] = files.values.flatten.toList

  /** Get the first file for a given field name. */
  def getFile(name: String): Option[FileUpload] =
    files.get(name).flatMap(_.headOption)

  /** Get all files for a given field name. */
  def getFiles(name: String): List[FileUpload] =
    files.getOrElse(name, List.empty)

  /** Total size of all uploaded files in bytes. */
  def totalFileSize: Long = allFiles.map(_.size).sum

  /** Number of uploaded files. */
  def fileCount: Int = allFiles.size
}

object MultipartData {

  /** Empty multipart data with no fields or files. */
  val empty: MultipartData = MultipartData(Map.empty, Map.empty)
}

/** Represents an uploaded file from a multipart/form-data request.
  *
  * @param fieldName
  *   The name attribute of the file input field
  * @param filename
  *   The original filename from the client (if provided)
  * @param contentType
  *   The MIME type of the file (from Content-Type header in the part)
  * @param data
  *   The file contents as a byte array
  * @param size
  *   Size of the file in bytes
  */
case class FileUpload(
    fieldName: String,
    filename: Option[String],
    contentType: Option[String],
    data: Array[Byte],
    size: Long
) {

  /** Check if this file matches a specific content type. */
  def hasContentType(mimeType: String): Boolean =
    contentType.exists(_.equalsIgnoreCase(mimeType))

  /** Check if this file has any of the given content types. */
  def hasAnyContentType(mimeTypes: Set[String]): Boolean =
    contentType.exists(ct => mimeTypes.exists(_.equalsIgnoreCase(ct)))

  /** Get the file extension from the filename (if available). */
  def extension: Option[String] = filename.flatMap { name =>
    val idx = name.lastIndexOf('.')
    // Extension must have a dot that's not at the start (idx > 0) and not at the end
    if (idx > 0 && idx < name.length - 1)
      Some(name.substring(idx + 1).toLowerCase)
    else None
  }
}

/** Represents a parsed multipart/form-data request with streaming file support.
  *
  * Used for large file uploads where buffering the entire file in memory would
  * be problematic. Files are either saved to temporary locations or processed
  * via callbacks during parsing.
  *
  * @param fields
  *   Map of field names to their text values
  * @param files
  *   Map of field names to lists of streaming file uploads
  */
case class MultipartDataStreaming(
    fields: Map[String, String],
    files: Map[String, List[StreamingFileUpload]]
) {

  /** Get all files as a flat list. */
  def allFiles: List[StreamingFileUpload] =
    files.values.flatten.toList

  /** Get the first file for a given field name. */
  def getFile(name: String): Option[StreamingFileUpload] =
    files.get(name).flatMap(_.headOption)

  /** Get all files for a given field name. */
  def getFiles(name: String): List[StreamingFileUpload] =
    files.getOrElse(name, List.empty)

  /** Total size of all uploaded files in bytes (if known). */
  def totalFileSize: Option[Long] = {
    val sizes = allFiles.flatMap(_.size)
    if (sizes.size == allFiles.size) Some(sizes.sum)
    else None
  }

  /** Number of uploaded files. */
  def fileCount: Int = allFiles.size
}

/** Represents an uploaded file with streaming/lazy access to the file data.
  *
  * The file data is not immediately loaded into memory. Instead, it can be
  * accessed via an InputStream when needed.
  *
  * @param fieldName
  *   The name attribute of the file input field
  * @param filename
  *   The original filename from the client (if provided)
  * @param contentType
  *   The MIME type of the file (from Content-Type header in the part)
  * @param dataStream
  *   Function that returns an InputStream for reading the file data
  * @param size
  *   Size of the file in bytes (may be unknown for chunked uploads)
  */
case class StreamingFileUpload(
    fieldName: String,
    filename: Option[String],
    contentType: Option[String],
    dataStream: () => InputStream,
    size: Option[Long]
) {

  /** Check if this file matches a specific content type. */
  def hasContentType(mimeType: String): Boolean =
    contentType.exists(_.equalsIgnoreCase(mimeType))

  /** Check if this file has any of the given content types. */
  def hasAnyContentType(mimeTypes: Set[String]): Boolean =
    contentType.exists(ct => mimeTypes.exists(_.equalsIgnoreCase(ct)))

  /** Get the file extension from the filename (if available). */
  def extension: Option[String] = filename.flatMap { name =>
    val idx = name.lastIndexOf('.')
    // Extension must have a dot that's not at the start (idx > 0) and not at the end
    if (idx > 0 && idx < name.length - 1)
      Some(name.substring(idx + 1).toLowerCase)
    else None
  }

  /** Read the entire file into memory as a byte array.
    *
    * WARNING: Only use this for small files. For large files, use the
    * dataStream directly.
    */
  def readAll(): Array[Byte] = {
    val stream = dataStream()
    try {
      stream.readAllBytes()
    } finally {
      stream.close()
    }
  }
}
