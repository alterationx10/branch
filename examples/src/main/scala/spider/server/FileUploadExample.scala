package spider.server

import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.common.HttpMethod.{GET, POST}
import dev.alteration.branch.spider.server.*
import dev.alteration.branch.spider.server.Response.html
import dev.alteration.branch.spider.server.BodyParser.*
import java.nio.file.{Files, Paths}

/** Example demonstrating multipart/form-data file upload handling.
  *
  * This example shows:
  * - Basic file upload handling
  * - Multiple file uploads
  * - File type validation with allowedFileTypes
  * - Configurable size limits
  *
  * To test with curl:
  * ```bash
  * # Single file upload
  * curl -X POST http://localhost:9000/upload \
  *   -F "title=My Photo" \
  *   -F "photo=@/path/to/image.jpg"
  *
  * # Multiple files
  * curl -X POST http://localhost:9000/upload \
  *   -F "photos=@photo1.jpg" \
  *   -F "photos=@photo2.jpg"
  * ```
  */
object FileUploadExample {

  import RequestHandler.given

  def main(args: Array[String]): Unit = {

    // Create upload directory if it doesn't exist
    val uploadDir = Paths.get("uploads")
    if (!Files.exists(uploadDir)) {
      Files.createDirectories(uploadDir)
    }

    // Home page with upload form
    val homeHandler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        html"""
        <!DOCTYPE html>
        <html>
        <head>
          <title>File Upload Example</title>
          <style>
            body { font-family: Arial, sans-serif; max-width: 800px; margin: 50px auto; padding: 20px; }
            form { border: 1px solid #ccc; padding: 20px; margin: 20px 0; border-radius: 8px; }
            input, button { margin: 10px 0; padding: 10px; display: block; width: 100%; box-sizing: border-box; }
            button { background: #007bff; color: white; border: none; cursor: pointer; border-radius: 4px; }
            button:hover { background: #0056b3; }
            .info { background: #e7f3ff; padding: 15px; border-left: 4px solid #007bff; margin: 20px 0; }
            label { display: block; margin: 10px 0; }
          </style>
        </head>
        <body>
          <h1>Multipart File Upload Example</h1>

          <div class="info">
            <strong>Features:</strong> Multipart/form-data parser with file type validation,
            size limits, and support for multiple files per field.
          </div>

          <form action="/upload" method="post" enctype="multipart/form-data">
            <h2>Upload Files</h2>
            <label>Title: <input type="text" name="title" required></label>
            <label>Photos: <input type="file" name="photos" multiple required></label>
            <button type="submit">Upload</button>
          </form>

          <form action="/profile" method="post" enctype="multipart/form-data">
            <h2>Profile Picture (Images Only, Max 5MB)</h2>
            <label>Picture: <input type="file" name="picture" accept="image/*" required></label>
            <button type="submit">Upload Profile Picture</button>
          </form>
        </body>
        </html>
        """
      }
    }

    // File upload handler
    val uploadHandler = new RequestHandler[Array[Byte], String] {
      def handle(request: Request[Array[Byte]]): Response[String] = {
        request.parseMultipartBody() match {
          case ParseSuccess(multipart) =>
            val title = multipart.fields.getOrElse("title", "Untitled")
            val photos = multipart.getFiles("photos")

            if (photos.isEmpty) {
              Response(400, "<h1>Error</h1><p>No files uploaded</p><p><a href='/'>Back</a></p>")
            } else {
              // Save all photos
              photos.foreach { photo =>
                val safeFilename = photo.filename.getOrElse("upload.bin")
                  .replaceAll("[^a-zA-Z0-9._-]", "_")
                val targetPath = uploadDir.resolve(safeFilename)
                Files.write(targetPath, photo.data)
              }

              val fileList = photos.map { p =>
                s"<li>${p.filename.getOrElse("unknown")} (${p.size} bytes, ${p.contentType.getOrElse("unknown")})</li>"
              }.mkString("\n")

              html"""
              <!DOCTYPE html>
              <html>
              <body style="font-family: Arial; max-width: 800px; margin: 50px auto; padding: 20px;">
                <h1>Upload Successful!</h1>
                <p><strong>Title:</strong> $title</p>
                <p><strong>Files uploaded:</strong> ${photos.size}</p>
                <ul>
                  $fileList
                </ul>
                <p><a href="/">Back to upload form</a></p>
              </body>
              </html>
              """
            }

          case BodyTooLarge =>
            Response(413, "Upload too large. Maximum size is 50MB.")

          case ParseFailure(error) =>
            Response(400, s"Upload failed: $error")

          case UnsupportedContentType =>
            Response(415, "Please use multipart/form-data")
        }
      }
    }

    // Profile picture handler with validation
    val profileHandler = new RequestHandler[Array[Byte], String] {
      def handle(request: Request[Array[Byte]]): Response[String] = {
        // Strict config with file type restrictions
        val config = ParserConfig.default.copy(
          maxFileSize = 5 * 1024 * 1024, // 5MB max
          maxFileCount = 1,
          allowedFileTypes = Some(Set("image/jpeg", "image/png", "image/gif", "image/webp"))
        )

        request.parseMultipartBody(config) match {
          case ParseSuccess(multipart) =>
            multipart.getFile("picture") match {
              case Some(picture) =>
                val filename = s"profile_${System.currentTimeMillis()}.${picture.extension.getOrElse("bin")}"
                val targetPath = uploadDir.resolve(filename)
                Files.write(targetPath, picture.data)

                html"""
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial; max-width: 800px; margin: 50px auto; padding: 20px;">
                  <h1>Profile Picture Updated!</h1>
                  <p><strong>Filename:</strong> ${picture.filename.getOrElse("unknown")}</p>
                  <p><strong>Type:</strong> ${picture.contentType.getOrElse("unknown")}</p>
                  <p><strong>Size:</strong> ${picture.size} bytes</p>
                  <p><strong>Saved as:</strong> $filename</p>
                  <p><a href="/">Back to upload form</a></p>
                </body>
                </html>
                """

              case None =>
                Response(400, "No picture uploaded")
            }

          case BodyTooLarge =>
            Response(413, "Profile picture too large (max 5MB)")

          case ParseFailure(error) =>
            Response(400, s"<h1>Error</h1><p>$error</p><p>Only images are allowed.</p><p><a href='/'>Back</a></p>")

          case UnsupportedContentType =>
            Response(415, "Please use multipart/form-data")
        }
      }
    }

    // Create server with routing
    val appServer = new SpiderApp {
      override val port: Int = 9000

      override val config: ServerConfig = ServerConfig.default.copy(
        maxRequestBodySize = Some(50 * 1024 * 1024) // 50MB max
      )

      override val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
        case (GET, Nil)                    => homeHandler
        case (POST, "upload" :: Nil)       => uploadHandler
        case (POST, "profile" :: Nil)      => profileHandler
      }
    }

    println()
    println("=" * 60)
    println("File Upload Server Started!")
    println("=" * 60)
    println()
    println("Open your browser to:")
    println("  http://localhost:9000/")
    println()
    println("Or test with curl:")
    println("  curl -X POST http://localhost:9000/upload \\")
    println("    -F 'title=Test' \\")
    println("    -F 'photos=@file1.jpg' \\")
    println("    -F 'photos=@file2.jpg'")
    println()
    println("Uploaded files are saved to: ./uploads/")
    println("Press Ctrl+C to stop the server.")
    println()

    appServer.main(Array.empty)
  }
}
