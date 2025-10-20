---
title: Spider Body Parsing
description: Parsing request and response bodies
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-01-25T04:38:00Z
tags:
  - http
  - parsing
  - json
  - forms
---

# Request/Response Body Parsing

Spider provides comprehensive utilities for parsing HTTP request bodies including JSON, forms, and multipart file uploads.

## Content Types

Spider supports parsing these content types:
- `application/json` - JSON data
- `application/x-www-form-urlencoded` - HTML form data
- `multipart/form-data` - File uploads with form fields
- `text/plain` - Plain text

## Form URL-Encoded

Parse HTML form data:

```scala
import dev.alteration.branch.spider.server.BodyParser

case class FormHandler() extends RequestHandler[Array[Byte], String] {
  override def handle(request: Request[Array[Byte]]): Response[String] = {
    // Parse form data
    val formData = request.parseFormBody

    val username = formData.get("username")
    val email = formData.get("email")

    Response(200, s"Received: $username, $email")
  }
}
```

With size limits:

```scala
val config = BodyParser.ParserConfig.default

request.parseFormBodySafe(config) match {
  case BodyParser.ParseSuccess(form) =>
    Response(200, s"Data: $form")

  case BodyParser.BodyTooLarge =>
    Response(413, "Form data too large")

  case BodyParser.ParseFailure(error) =>
    Response(400, s"Parse error: $error")

  case BodyParser.UnsupportedContentType =>
    Response(415, "Unsupported content type")
}
```

## JSON Parsing

Parse JSON request bodies:

```scala
import dev.alteration.branch.friday.JsonDecoder
import dev.alteration.branch.spider.server.BodyParser

case class User(name: String, email: String, age: Int) derives JsonDecoder

case class JsonHandler() extends RequestHandler[Array[Byte], String] {
  override def handle(request: Request[Array[Byte]]): Response[String] = {
    request.parseJsonBody[User] match {
      case Success(user) =>
        Response(200, s"Hello, ${user.name}!")

      case Failure(error) =>
        Response(400, s"Invalid JSON: ${error.getMessage}")
    }
  }
}
```

With size limits:

```scala
val config = BodyParser.ParserConfig.default

request.parseJsonBodySafe[User](config) match {
  case BodyParser.ParseSuccess(user) =>
    Response(200, s"Created user: ${user.name}")

  case BodyParser.BodyTooLarge =>
    Response(413, "JSON payload too large")

  case BodyParser.ParseFailure(error) =>
    Response(400, s"Invalid JSON: $error")

  case BodyParser.UnsupportedContentType =>
    Response(415, "Expected application/json")
}
```

## Automatic Parsing

Parse based on Content-Type header:

```scala
import dev.alteration.branch.friday.JsonDecoder

case class User(name: String, email: String) derives JsonDecoder

case class AutoHandler() extends RequestHandler[Array[Byte], String] {
  override def handle(request: Request[Array[Byte]]): Response[String] = {
    request.parseBodyAuto[User]() match {
      case BodyParser.ParseSuccess(Left(formData)) =>
        // Form data
        Response(200, s"Form: ${formData.get("name")}")

      case BodyParser.ParseSuccess(Right(user)) =>
        // JSON data
        Response(200, s"JSON: ${user.name}")

      case BodyParser.BodyTooLarge =>
        Response(413, "Payload too large")

      case BodyParser.ParseFailure(error) =>
        Response(400, s"Parse error: $error")

      case BodyParser.UnsupportedContentType =>
        Response(415, "Unsupported content type")
    }
  }
}
```

## Multipart File Uploads

Parse file uploads with form fields:

```scala
import dev.alteration.branch.spider.server.{BodyParser, MultipartData, FileUpload}

case class UploadHandler() extends RequestHandler[Array[Byte], String] {
  override def handle(request: Request[Array[Byte]]): Response[String] = {
    request.parseMultipartBody() match {
      case BodyParser.ParseSuccess(multipart) =>
        // Access form fields
        val description = multipart.fields.get("description")

        // Access uploaded files
        val files = multipart.files.get("file")

        files match {
          case Some(uploads) =>
            uploads.foreach { upload =>
              val filename = upload.filename.getOrElse("unknown")
              val contentType = upload.contentType.getOrElse("unknown")
              val size = upload.size

              // Save file
              saveFile(upload.data, filename)

              println(s"Uploaded: $filename ($contentType, $size bytes)")
            }

            Response(200, s"Uploaded ${uploads.size} file(s)")

          case None =>
            Response(400, "No file uploaded")
        }

      case BodyParser.BodyTooLarge =>
        Response(413, "Upload too large")

      case BodyParser.ParseFailure(error) =>
        Response(400, s"Upload failed: $error")

      case _ =>
        Response(400, "Invalid upload")
    }
  }
}
```

### Multipart Configuration

Configure upload limits:

```scala
val config = BodyParser.ParserConfig(
  maxMultipartSize = 50 * 1024 * 1024,  // 50MB total
  maxFileSize = 20 * 1024 * 1024,       // 20MB per file
  maxFileCount = 10,                     // Max 10 files
  allowedFileTypes = Some(Set(
    "image/jpeg",
    "image/png",
    "image/gif",
    "application/pdf"
  ))
)

request.parseMultipartBody(config) match {
  case BodyParser.ParseSuccess(multipart) =>
    processUpload(multipart)

  case error =>
    handleError(error)
}
```

### Accessing Uploaded Files

The `MultipartData` contains:

```scala
case class MultipartData(
  fields: Map[String, String],           // Form fields
  files: Map[String, List[FileUpload]]   // Uploaded files by field name
)

case class FileUpload(
  fieldName: String,                     // Form field name
  filename: Option[String],              // Original filename
  contentType: Option[String],           // MIME type
  data: Array[Byte],                     // File content
  size: Long                             // File size in bytes
)
```

## Parser Configuration

### Default Configuration

```scala
val config = BodyParser.ParserConfig.default

// Limits:
// - JSON: 10MB
// - Forms: 1MB
// - Text: 5MB
// - Multipart total: 50MB
// - File size: 20MB per file
// - File count: 10 files max
```

### Strict Configuration

```scala
val config = BodyParser.ParserConfig.strict

// Limits:
// - JSON: 1MB
// - Forms: 512KB
// - Text: 1MB
// - Multipart total: 10MB
// - File size: 5MB per file
// - File count: 5 files max
```

### Permissive Configuration

```scala
val config = BodyParser.ParserConfig.permissive

// Limits:
// - JSON: 50MB
// - Forms: 10MB
// - Text: 25MB
// - Multipart total: 200MB
// - File size: 100MB per file
// - File count: 50 files max
```

### Custom Configuration

```scala
val config = BodyParser.ParserConfig(
  maxJsonSize = 5 * 1024 * 1024,         // 5MB
  maxFormSize = 1 * 1024 * 1024,         // 1MB
  maxTextSize = 2 * 1024 * 1024,         // 2MB
  maxMultipartSize = 100 * 1024 * 1024,  // 100MB
  maxFileSize = 50 * 1024 * 1024,        // 50MB per file
  maxFileCount = 20,                      // 20 files max
  allowedFileTypes = Some(Set(
    "image/jpeg",
    "image/png",
    "application/pdf"
  ))
)
```

## Helper Methods

### Content Type Detection

```scala
// Get content type
val contentType = request.contentType
// Some("application/json") or None

// Get multipart boundary
val boundary = request.multipartBoundary
// Some("----WebKitFormBoundary...") or None
```

### Size Validation

```scala
// Check body size
val size = request.bodySizeBytes

// Check if within limit
if (request.isBodyWithinLimit(10 * 1024 * 1024)) {
  // Process body
} else {
  Response(413, "Request too large")
}
```

## Complete Example

```scala
import dev.alteration.branch.spider.server.*
import dev.alteration.branch.friday.JsonDecoder

case class User(name: String, email: String) derives JsonDecoder

case class UserHandler() extends RequestHandler[Array[Byte], String] {
  val config = BodyParser.ParserConfig.default

  override def handle(request: Request[Array[Byte]]): Response[String] = {
    request.contentType match {
      case Some("application/json") =>
        handleJson(request)

      case Some("application/x-www-form-urlencoded") =>
        handleForm(request)

      case Some("multipart/form-data") =>
        handleUpload(request)

      case _ =>
        Response(415, "Unsupported content type")
    }
  }

  def handleJson(request: Request[Array[Byte]]): Response[String] = {
    request.parseJsonBodySafe[User](config) match {
      case BodyParser.ParseSuccess(user) =>
        Response(200, s"Created user: ${user.name}")

      case BodyParser.BodyTooLarge =>
        Response(413, "JSON too large")

      case BodyParser.ParseFailure(error) =>
        Response(400, s"Invalid JSON: $error")

      case _ =>
        Response(400, "Bad request")
    }
  }

  def handleForm(request: Request[Array[Byte]]): Response[String] = {
    request.parseFormBodySafe(config) match {
      case BodyParser.ParseSuccess(form) =>
        val name = form.get("name")
        val email = form.get("email")
        Response(200, s"Form data: $name, $email")

      case error =>
        Response(400, "Form parse error")
    }
  }

  def handleUpload(request: Request[Array[Byte]]): Response[String] = {
    request.parseMultipartBody(config) match {
      case BodyParser.ParseSuccess(multipart) =>
        val fileCount = multipart.files.values.flatten.size
        Response(200, s"Uploaded $fileCount file(s)")

      case BodyParser.BodyTooLarge =>
        Response(413, "Upload too large")

      case error =>
        Response(400, "Upload failed")
    }
  }
}
```

## Error Handling

Use helper methods for standard error responses:

```scala
import dev.alteration.branch.spider.server.BodyParser

request.parseJsonBody[User] match {
  case Success(user) =>
    Response(200, s"OK: ${user.name}")

  case Failure(_) =>
    BodyParser.badRequestResponse("Invalid JSON")
}

// Or for size limits
if (!request.isBodyWithinLimit(config.maxJsonSize)) {
  BodyParser.payloadTooLargeResponse
} else {
  // Process request
}

// Or for unsupported content types
request.contentType match {
  case Some("application/json") =>
    // Handle JSON
  case _ =>
    BodyParser.unsupportedMediaTypeResponse
}
```

## Next Steps

- Learn about [Streaming](/spider/streaming) for large file uploads
- Explore [Middleware](/spider/middleware) for request processing
- Return to [HTTP Server](/spider/server)
