package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.hollywood.tools.provided.FileSystemTool

import java.nio.file.{Path, Paths}
import scala.util.Try

/** Safety policy for FileSystemTool operations
  *
  * @param sandboxPath
  *   Optional root directory - all file operations must be within this path
  * @param readOnly
  *   If true, only read, list, and exists operations are allowed
  * @param maxFileSize
  *   Maximum file size in bytes for write operations (default 10MB)
  * @param blockedPatterns
  *   List of path patterns that are blocked (e.g., ".env", ".key", ".ssh")
  */
case class FileSystemPolicy(
    sandboxPath: Option[Path] = None,
    readOnly: Boolean = false,
    maxFileSize: Long = 10 * 1024 * 1024, // 10MB default
    blockedPatterns: List[String] = List(
      ".env",
      ".key",
      ".pem",
      ".crt",
      "credentials",
      "password",
      ".ssh"
    )
) extends ToolPolicy[FileSystemTool] {

  /** Validate a FileSystemTool invocation against this policy */
  override def validate(tool: FileSystemTool): Try[Unit] = {
    val contentSize = tool.content.map(_.length.toLong)
    validatePath(tool.operation, tool.path, contentSize)
  }

  /** Validate a file path against this policy
    *
    * @param operation
    *   The operation being performed (read, write, list, exists)
    * @param pathStr
    *   The path to validate
    * @param contentSize
    *   Optional content size for write operations
    * @return
    *   Success if valid, Failure with SecurityException if not
    */
  def validatePath(
      operation: String,
      pathStr: String,
      contentSize: Option[Long] = None
  ): Try[Unit] = Try {
    val path = Paths.get(pathStr).toAbsolutePath.normalize()

    // Sandbox validation
    sandboxPath.foreach { sandbox =>
      val normalizedSandbox = sandbox.toAbsolutePath.normalize()
      if (!path.startsWith(normalizedSandbox)) {
        throw new SecurityException(
          s"Path '$path' is outside sandbox '$normalizedSandbox'"
        )
      }
    }

    // Read-only validation
    if (readOnly && operation.toLowerCase == "write") {
      throw new SecurityException(
        "Write operations are disabled by policy (read-only mode)"
      )
    }

    // Blocked patterns validation
    val pathString = path.toString.toLowerCase
    blockedPatterns.foreach { pattern =>
      if (pathString.contains(pattern.toLowerCase)) {
        throw new SecurityException(
          s"Path contains blocked pattern '$pattern': $path"
        )
      }
    }

    // File size validation for write operations
    if (operation.toLowerCase == "write") {
      contentSize.foreach { size =>
        if (size > maxFileSize) {
          throw new SecurityException(
            s"Content size ($size bytes) exceeds maximum allowed ($maxFileSize bytes)"
          )
        }
      }
    }
  }
}

object FileSystemPolicy {

  /** A strict policy: read-only, sandboxed to /tmp, common blocked patterns */
  def strict(sandboxPath: Path): FileSystemPolicy =
    FileSystemPolicy(
      sandboxPath = Some(sandboxPath),
      readOnly = true
    )

  /** A permissive policy: writes allowed, larger file size, minimal blocks */
  def permissive(sandboxPath: Option[Path] = None): FileSystemPolicy =
    FileSystemPolicy(
      sandboxPath = sandboxPath,
      readOnly = false,
      maxFileSize = 100 * 1024 * 1024, // 100MB
      blockedPatterns = List(".env", ".key", ".pem") // Minimal blocks
    )

  /** Default policy: sandboxed with reasonable restrictions */
  def default(sandboxPath: Path): FileSystemPolicy =
    FileSystemPolicy(sandboxPath = Some(sandboxPath))
}
