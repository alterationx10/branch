package dev.alteration.branch.spider.server

import munit.FunSuite
import java.nio.charset.StandardCharsets
import java.net.URI

class MultipartParserSpec extends FunSuite {

  import BodyParser.*

  // Helper to create a simple multipart body
  def createSimpleMultipart(boundary: String): Array[Byte] = {
    val body = s"""--$boundary\r
Content-Disposition: form-data; name="field1"\r
\r
value1\r
--$boundary\r
Content-Disposition: form-data; name="field2"\r
\r
value2\r
--$boundary--\r
"""
    body.getBytes(StandardCharsets.UTF_8)
  }

  // Helper to create multipart with file
  def createMultipartWithFile(boundary: String): Array[Byte] = {
    val fileContent = "Hello, World!"
    val body        = s"""--$boundary\r
Content-Disposition: form-data; name="title"\r
\r
My Photo\r
--$boundary\r
Content-Disposition: form-data; name="photo"; filename="test.txt"\r
Content-Type: text/plain\r
\r
$fileContent\r
--$boundary--\r
"""
    body.getBytes(StandardCharsets.UTF_8)
  }

  // Helper to create multipart with multiple files
  def createMultipartWithMultipleFiles(boundary: String): Array[Byte] = {
    val body = s"""--$boundary\r
Content-Disposition: form-data; name="photos"; filename="file1.txt"\r
Content-Type: text/plain\r
\r
First file content\r
--$boundary\r
Content-Disposition: form-data; name="photos"; filename="file2.txt"\r
Content-Type: text/plain\r
\r
Second file content\r
--$boundary--\r
"""
    body.getBytes(StandardCharsets.UTF_8)
  }

  // Helper to create multipart with binary data
  def createMultipartWithBinaryData(boundary: String): Array[Byte] = {
    val textPart = s"""--$boundary\r
Content-Disposition: form-data; name="description"\r
\r
Binary file\r
--$boundary\r
Content-Disposition: form-data; name="data"; filename="binary.bin"\r
Content-Type: application/octet-stream\r
\r
""".getBytes(StandardCharsets.UTF_8)

    val binaryData = Array[Byte](0, 1, 2, 3, 255.toByte, 254.toByte, 127)
    val endPart    = s"""\r
--$boundary--\r
""".getBytes(StandardCharsets.UTF_8)

    textPart ++ binaryData ++ endPart
  }

  test("parseMultipart should parse simple text fields") {
    val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    val body     = createSimpleMultipart(boundary)
    val config   = ParserConfig.default

    val result = MultipartParser.parseMultipart(body, boundary, config)

    assert(result.isSuccess)
    val data = result.get
    assertEquals(data.fields.size, 2)
    assertEquals(data.fields.get("field1"), Some("value1"))
    assertEquals(data.fields.get("field2"), Some("value2"))
    assertEquals(data.files.size, 0)
  }

  test("parseMultipart should parse text field and file") {
    val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    val body     = createMultipartWithFile(boundary)
    val config   = ParserConfig.default

    val result = MultipartParser.parseMultipart(body, boundary, config)

    assert(result.isSuccess)
    val data = result.get
    assertEquals(data.fields.get("title"), Some("My Photo"))
    assertEquals(data.files.size, 1)

    val file = data.getFile("photo")
    assert(file.isDefined)
    assertEquals(file.get.filename, Some("test.txt"))
    assertEquals(file.get.contentType, Some("text/plain"))
    assertEquals(
      new String(file.get.data, StandardCharsets.UTF_8),
      "Hello, World!"
    )
  }

  test("parseMultipart should parse multiple files with same field name") {
    val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    val body     = createMultipartWithMultipleFiles(boundary)
    val config   = ParserConfig.default

    val result = MultipartParser.parseMultipart(body, boundary, config)

    assert(result.isSuccess)
    val data = result.get
    assertEquals(data.files.size, 1)

    val files = data.getFiles("photos")
    assertEquals(files.size, 2)
    assertEquals(files(0).filename, Some("file1.txt"))
    assertEquals(files(1).filename, Some("file2.txt"))
    assertEquals(
      new String(files(0).data, StandardCharsets.UTF_8),
      "First file content"
    )
    assertEquals(
      new String(files(1).data, StandardCharsets.UTF_8),
      "Second file content"
    )
  }

  test("parseMultipart should handle binary data correctly") {
    val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    val body     = createMultipartWithBinaryData(boundary)
    val config   = ParserConfig.default

    val result = MultipartParser.parseMultipart(body, boundary, config)

    assert(result.isSuccess)
    val data = result.get
    assertEquals(data.fields.get("description"), Some("Binary file"))

    val file = data.getFile("data")
    assert(file.isDefined)
    assertEquals(file.get.filename, Some("binary.bin"))
    assertEquals(file.get.contentType, Some("application/octet-stream"))
    assert(
      file.get.data.sameElements(
        Array[Byte](0, 1, 2, 3, 255.toByte, 254.toByte, 127)
      )
    )
  }

  test("parseMultipart should reject body exceeding maxMultipartSize") {
    val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    val body     = createSimpleMultipart(boundary)
    val config   = ParserConfig.default.copy(maxMultipartSize = 10) // Very small

    val result = MultipartParser.parseMultipart(body, boundary, config)

    assert(result.isFailure)
    assert(result.failed.get.getMessage.contains("too large"))
  }

  test("parseMultipart should reject file exceeding maxFileSize") {
    val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    val body     = createMultipartWithFile(boundary)
    val config   =
      ParserConfig.default.copy(maxFileSize = 5) // Smaller than "Hello, World!"

    val result = MultipartParser.parseMultipart(body, boundary, config)

    assert(result.isFailure)
    assert(result.failed.get.getMessage.contains("File"))
    assert(result.failed.get.getMessage.contains("too large"))
  }

  test("parseMultipart should reject too many files") {
    val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    val body     = createMultipartWithMultipleFiles(boundary)
    val config   = ParserConfig.default.copy(maxFileCount = 1)

    val result = MultipartParser.parseMultipart(body, boundary, config)

    assert(result.isFailure)
    assert(result.failed.get.getMessage.contains("Too many files"))
  }

  test("parseMultipart should enforce allowedFileTypes") {
    val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    val body     = createMultipartWithFile(boundary)
    val config   = ParserConfig.default.copy(
      allowedFileTypes = Some(Set("image/jpeg", "image/png"))
    )

    val result = MultipartParser.parseMultipart(body, boundary, config)

    assert(result.isFailure)
    assert(result.failed.get.getMessage.contains("File type not allowed"))
  }

  test("parseMultipart should accept allowed file types") {
    val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    val body     = createMultipartWithFile(boundary)
    val config   = ParserConfig.default.copy(
      allowedFileTypes = Some(Set("text/plain", "image/jpeg"))
    )

    val result = MultipartParser.parseMultipart(body, boundary, config)

    assert(result.isSuccess)
  }

  test("parseMultipart should handle multipart with only empty text field") {
    val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    // Multipart with a valid but empty field
    val body     = s"""--$boundary\r
Content-Disposition: form-data; name="emptyfield"\r
\r
\r
--$boundary--\r
""".getBytes(StandardCharsets.UTF_8)
    val config   = ParserConfig.default

    val result = MultipartParser.parseMultipart(body, boundary, config)

    assert(result.isSuccess)
    val data = result.get
    assertEquals(data.fields.size, 1)
    assertEquals(data.fields.get("emptyfield"), Some(""))
    assertEquals(data.files.size, 0)
  }

  test("parseMultipart should handle quoted boundary") {
    val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    val body     = createSimpleMultipart(boundary)
    val config   = ParserConfig.default

    // Boundary should work the same whether quoted or not
    val result = MultipartParser.parseMultipart(body, boundary, config)
    assert(result.isSuccess)
  }

  test("Request.parseMultipartBody should parse multipart request") {
    val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    val body     = createMultipartWithFile(boundary)
    val request  = Request(
      uri = URI.create("http://localhost/upload"),
      headers = Map(
        "Content-Type" -> List(s"multipart/form-data; boundary=$boundary")
      ),
      body = body
    )

    val result = request.parseMultipartBody()

    result match {
      case ParseSuccess(data)     =>
        assertEquals(data.fields.get("title"), Some("My Photo"))
        assertEquals(
          data.getFile("photo").map(_.filename),
          Some(Some("test.txt"))
        )
      case ParseFailure(error)    =>
        fail(s"Expected success but got failure: $error")
      case BodyTooLarge           =>
        fail("Body too large")
      case UnsupportedContentType =>
        fail("Unsupported content type")
    }
  }

  test("Request.parseMultipartBody should fail without boundary") {
    val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    val body     = createMultipartWithFile(boundary)
    val request  = Request(
      uri = URI.create("http://localhost/upload"),
      headers = Map(
        "Content-Type" -> List("multipart/form-data") // Missing boundary
      ),
      body = body
    )

    val result = request.parseMultipartBody()

    result match {
      case ParseFailure(error) =>
        assert(error.contains("boundary"))
      case _                   =>
        fail("Expected ParseFailure")
    }
  }

  test("Request.multipartBoundary should extract boundary from Content-Type") {
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map(
        "Content-Type" -> List(
          "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW"
        )
      ),
      body = Array.empty[Byte]
    )

    assertEquals(
      request.multipartBoundary,
      Some("----WebKitFormBoundary7MA4YWxkTrZu0gW")
    )
  }

  test("Request.multipartBoundary should extract quoted boundary") {
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map(
        "Content-Type" -> List(
          "multipart/form-data; boundary=\"----WebKitFormBoundary7MA4YWxkTrZu0gW\""
        )
      ),
      body = Array.empty[Byte]
    )

    assertEquals(
      request.multipartBoundary,
      Some("----WebKitFormBoundary7MA4YWxkTrZu0gW")
    )
  }

  test("Request.multipartBoundary should return None if boundary missing") {
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map(
        "Content-Type" -> List("multipart/form-data")
      ),
      body = Array.empty[Byte]
    )

    assertEquals(request.multipartBoundary, None)
  }

  test("Request.parseBodyAuto should handle multipart and return only fields") {
    val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    val body     = createMultipartWithFile(boundary)
    val request  = Request(
      uri = URI.create("http://localhost/upload"),
      headers = Map(
        "Content-Type" -> List(s"multipart/form-data; boundary=$boundary")
      ),
      body = body
    )

    // Need a dummy decoder for parseBodyAuto
    import dev.alteration.branch.friday.{JsonDecoder, Json}
    given JsonDecoder[String] = new JsonDecoder[String] {
      override def decode(json: Json)   = ???
      override def decode(json: String) = ???
    }

    val result = request.parseBodyAuto[String]()

    result match {
      case ParseSuccess(Left(fields)) =>
        // parseBodyAuto returns only text fields, not files
        assertEquals(fields.get("title"), Some("My Photo"))
        assert(!fields.contains("photo")) // Files are not included
      case _                          =>
        fail("Expected ParseSuccess with Left(fields)")
    }
  }

  test("FileUpload helper methods should work correctly") {
    val upload = FileUpload(
      fieldName = "photo",
      filename = Some("test.jpg"),
      contentType = Some("image/jpeg"),
      data = Array.empty[Byte],
      size = 0
    )

    assert(upload.hasContentType("image/jpeg"))
    assert(!upload.hasContentType("image/png"))
    assert(upload.hasAnyContentType(Set("image/jpeg", "image/png")))
    assert(!upload.hasAnyContentType(Set("text/plain", "application/json")))
    assertEquals(upload.extension, Some("jpg"))
  }

  test("FileUpload should extract extension correctly") {
    val cases = List(
      ("file.txt", Some("txt")),
      ("file.tar.gz", Some("gz")),
      ("no-extension", None),
      ("file.", None),
      (".hidden", None)
    )

    for ((filename, expectedExt) <- cases) {
      val upload = FileUpload("field", Some(filename), None, Array.empty, 0)
      assertEquals(
        upload.extension,
        expectedExt,
        s"Failed for filename: $filename"
      )
    }
  }

  test("MultipartData helper methods should work correctly") {
    val file1 =
      FileUpload("photos", Some("1.jpg"), Some("image/jpeg"), Array.empty, 100)
    val file2 =
      FileUpload("photos", Some("2.jpg"), Some("image/jpeg"), Array.empty, 200)
    val file3 = FileUpload(
      "document",
      Some("doc.pdf"),
      Some("application/pdf"),
      Array.empty,
      300
    )

    val data = MultipartData(
      fields = Map("title" -> "Test"),
      files = Map(
        "photos"   -> List(file1, file2),
        "document" -> List(file3)
      )
    )

    assertEquals(data.fileCount, 3)
    assertEquals(data.totalFileSize, 600L)
    assertEquals(data.getFiles("photos").size, 2)
    assertEquals(data.getFile("document"), Some(file3))
    assertEquals(data.allFiles.size, 3)
  }

  test("parseMultipart should handle filename with special characters") {
    val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    val body     = s"""--$boundary\r
Content-Disposition: form-data; name="file"; filename="my file (1).txt"\r
Content-Type: text/plain\r
\r
content\r
--$boundary--\r
""".getBytes(StandardCharsets.UTF_8)

    val result =
      MultipartParser.parseMultipart(body, boundary, ParserConfig.default)

    assert(result.isSuccess)
    val file = result.get.getFile("file")
    assertEquals(file.map(_.filename), Some(Some("my file (1).txt")))
  }

  test("parseMultipart should handle parts without filename (text fields)") {
    val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    val body     = s"""--$boundary\r
Content-Disposition: form-data; name="message"\r
\r
Hello World\r
--$boundary--\r
""".getBytes(StandardCharsets.UTF_8)

    val result =
      MultipartParser.parseMultipart(body, boundary, ParserConfig.default)

    assert(result.isSuccess)
    assertEquals(result.get.fields.get("message"), Some("Hello World"))
    assertEquals(result.get.files.size, 0)
  }
}
