package spider.server

import dev.alteration.branch.spider.server.{
  SpiderApp,
  RequestHandler,
  StreamingRequestHandler,
  Request,
  Response,
  StreamingRequest
}
import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.server.RequestHandler.given
import dev.alteration.branch.spider.server.Response.*

import java.io.FileOutputStream
import java.nio.file.{Files, Paths}

/** Example demonstrating streaming request body handling.
  *
  * This shows how to process large request bodies efficiently without
  * buffering them entirely in memory.
  *
  * Test with:
  *   # Upload a file using chunked encoding
  *   curl -X POST http://localhost:9000/upload \
  *     --data-binary @large-file.bin \
  *     -H "Content-Type: application/octet-stream"
  *
  *   # Upload with Content-Length
  *   curl -X POST http://localhost:9000/upload/with-name?name=test.bin \
  *     --data-binary @file.bin
  *
  *   # Test chunked request
  *   curl -X POST http://localhost:9000/upload/chunked \
  *     -H "Transfer-Encoding: chunked" \
  *     --data-binary @file.bin
  *
  *   # Count bytes in streaming request
  *   curl -X POST http://localhost:9000/count \
  *     --data-binary @file.bin
  */
object StreamingRequestExample extends SpiderApp {

  // Create uploads directory if it doesn't exist
  private val uploadsDir = Paths.get("uploads")
  Files.createDirectories(uploadsDir)

  // === Streaming Request Handlers ===

  /** Upload handler that streams the request body to a file.
    *
    * This processes the request body in chunks without loading it all into memory.
    */
  val uploadHandler = new StreamingRequestHandler[String] {
    override def handle(request: Request[StreamingRequest]): Response[String] = {
      val stream = request.body
      val fileName = s"upload-${System.currentTimeMillis()}.bin"
      val filePath = uploadsDir.resolve(fileName)

      var totalBytes = 0L

      try {
        val fileOutput = new FileOutputStream(filePath.toFile)

        try {
          // Read and write chunks as they arrive
          stream.readChunks(chunkSize = 8192) { chunk =>
            totalBytes += chunk.length
            fileOutput.write(chunk)
            println(s"Received chunk: ${chunk.length} bytes (total: $totalBytes)")
          }
        } finally {
          fileOutput.close()
        }

        Response(
          200,
          s"""Upload successful!
             |File: $fileName
             |Size: $totalBytes bytes
             |Chunked: ${stream.isChunked}
             |Saved to: ${filePath.toAbsolutePath}
             |""".stripMargin
        ).textContent

      } catch {
        case e: Exception =>
          // Clean up file on error
          Files.deleteIfExists(filePath)
          Response.internalServerError(s"Upload failed: ${e.getMessage}")
      }
    }
  }

  /** Upload handler with custom filename from query parameter.
    */
  val uploadWithNameHandler = new StreamingRequestHandler[String] {
    override def handle(request: Request[StreamingRequest]): Response[String] = {
      val stream = request.body

      // Get filename from query params
      val queryParams = Request.parseQueryParams(
        Option(request.uri.getQuery).getOrElse("")
      )
      val fileName = queryParams.getOrElse("name", s"upload-${System.currentTimeMillis()}.bin")
      val filePath = uploadsDir.resolve(fileName)

      var totalBytes = 0L

      try {
        val fileOutput = new FileOutputStream(filePath.toFile)

        try {
          // Use withReader for more control
          stream.withReader { reader =>
            var done = false
            while (!done) {
              val chunk = reader.read(maxBytes = 8192)
              if (chunk.isEmpty) {
                done = true
              } else {
                totalBytes += chunk.length
                fileOutput.write(chunk)
              }
            }
          }
        } finally {
          fileOutput.close()
        }

        Response(
          200,
          s"""Upload successful!
             |File: $fileName
             |Size: $totalBytes bytes
             |Content-Length: ${stream.contentLength.getOrElse("unknown")}
             |Saved to: ${filePath.toAbsolutePath}
             |""".stripMargin
        ).textContent

      } catch {
        case e: Exception =>
          Files.deleteIfExists(filePath)
          Response.internalServerError(s"Upload failed: ${e.getMessage}")
      }
    }
  }

  /** Simple byte counter that doesn't save the data.
    */
  val countHandler = new StreamingRequestHandler[String] {
    override def handle(request: Request[StreamingRequest]): Response[String] = {
      val stream = request.body
      var totalBytes = 0L
      var chunkCount = 0

      stream.readChunks(chunkSize = 4096) { chunk =>
        totalBytes += chunk.length
        chunkCount += 1
      }

      json"""
      {
        "totalBytes": $totalBytes,
        "chunkCount": $chunkCount,
        "isChunked": ${stream.isChunked},
        "contentLength": ${stream.contentLength.getOrElse(-1)}
      }
      """
    }
  }

  /** Handler that processes chunked encoding specifically.
    */
  val chunkedUploadHandler = new StreamingRequestHandler[String] {
    override def handle(request: Request[StreamingRequest]): Response[String] = {
      val stream = request.body

      if (!stream.isChunked) {
        return Response.badRequest("This endpoint requires chunked transfer encoding")
      }

      val fileName = s"chunked-${System.currentTimeMillis()}.bin"
      val filePath = uploadsDir.resolve(fileName)

      var totalBytes = 0L
      var chunkCount = 0

      try {
        val fileOutput = new FileOutputStream(filePath.toFile)

        try {
          stream.readChunks() { chunk =>
            totalBytes += chunk.length
            chunkCount += 1
            fileOutput.write(chunk)
            println(s"Received chunk #$chunkCount: ${chunk.length} bytes")
          }
        } finally {
          fileOutput.close()
        }

        Response(
          200,
          s"""Chunked upload successful!
             |File: $fileName
             |Chunks: $chunkCount
             |Total size: $totalBytes bytes
             |Saved to: ${filePath.toAbsolutePath}
             |""".stripMargin
        ).textContent

      } catch {
        case e: Exception =>
          Files.deleteIfExists(filePath)
          Response.internalServerError(s"Upload failed: ${e.getMessage}")
      }
    }
  }

  // === Regular Handlers ===

  val homeHandler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      html"""
      <!DOCTYPE html>
      <html>
      <head>
        <title>Streaming Request Example</title>
        <style>
          body { font-family: sans-serif; max-width: 800px; margin: 40px auto; padding: 0 20px; }
          h1 { color: #333; }
          h2 { color: #666; margin-top: 30px; }
          pre { background: #f4f4f4; padding: 15px; border-radius: 5px; overflow-x: auto; }
          code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; }
          .info { background: #e3f2fd; border-left: 4px solid #2196F3; padding: 15px; margin: 20px 0; }
        </style>
      </head>
      <body>
        <h1>Streaming Request Example</h1>
        <p>This example demonstrates efficient handling of large request bodies using streaming.</p>

        <div class="info">
          <strong>Key Benefits:</strong>
          <ul>
            <li>Process large uploads without buffering everything in memory</li>
            <li>Support for both Content-Length and chunked transfer encoding</li>
            <li>Real-time chunk processing</li>
            <li>Memory-efficient for large files</li>
          </ul>
        </div>

        <h2>Available Endpoints</h2>

        <h3>POST /upload</h3>
        <p>Upload a file with automatic naming. Supports both Content-Length and chunked encoding.</p>
        <pre>curl -X POST http://localhost:9000/upload --data-binary @file.bin</pre>

        <h3>POST /upload/with-name?name=filename.bin</h3>
        <p>Upload a file with custom name from query parameter.</p>
        <pre>curl -X POST "http://localhost:9000/upload/with-name?name=test.bin" --data-binary @file.bin</pre>

        <h3>POST /upload/chunked</h3>
        <p>Upload using chunked transfer encoding only.</p>
        <pre>curl -X POST http://localhost:9000/upload/chunked -H "Transfer-Encoding: chunked" --data-binary @file.bin</pre>

        <h3>POST /count</h3>
        <p>Count bytes without saving the data.</p>
        <pre>curl -X POST http://localhost:9000/count --data-binary @file.bin</pre>

        <h2>Test Data Generation</h2>
        <p>Generate test files:</p>
        <pre>
# Generate 10MB test file
dd if=/dev/urandom of=test-10mb.bin bs=1M count=10

# Generate 100MB test file
dd if=/dev/urandom of=test-100mb.bin bs=1M count=100
        </pre>

        <h2>Uploaded Files</h2>
        <p>Files are saved to: <code>${uploadsDir.toAbsolutePath}</code></p>
      </body>
      </html>
      """
    }
  }

  // === Routers ===

  override val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
    case (HttpMethod.GET, Nil) => homeHandler
  }

  override val streamingRouter: PartialFunction[(HttpMethod, List[String]), StreamingRequestHandler[?]] = {
    case (HttpMethod.POST, "upload" :: Nil) => uploadHandler
    case (HttpMethod.POST, "upload" :: "with-name" :: Nil) => uploadWithNameHandler
    case (HttpMethod.POST, "upload" :: "chunked" :: Nil) => chunkedUploadHandler
    case (HttpMethod.POST, "count" :: Nil) => countHandler
  }
}
