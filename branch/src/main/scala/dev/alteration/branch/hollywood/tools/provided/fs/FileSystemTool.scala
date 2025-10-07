package dev.alteration.branch.hollywood.tools.provided.fs

import dev.alteration.branch.hollywood.tools.schema.Param
import dev.alteration.branch.hollywood.tools.{CallableTool, schema}

import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.util.Try

@schema.Tool("Read, write, or list files on the filesystem")
case class FileSystemTool(
    @Param("Operation to perform: 'read', 'write', 'list', or 'exists'")
    operation: String,
    @Param("File or directory path") path: String,
    @Param(
      "Content to write (required for 'write' operation)"
    ) content: Option[String] = None,
    @Param(
      "Whether to append to file instead of overwriting (for 'write' operation)"
    ) append: Option[Boolean] = None
) extends CallableTool[String] {

  override def execute(): Try[String] = Try {
    val filePath = Paths.get(path)

    operation.toLowerCase match {
      case "read"   => readFile(filePath)
      case "write"  =>
        content match {
          case Some(text) => writeFile(filePath, text, append.getOrElse(false))
          case None       =>
            throw new IllegalArgumentException(
              "Content parameter is required for write operation"
            )
        }
      case "list"   => listDirectory(filePath)
      case "exists" => checkExists(filePath)
      case _        =>
        throw new IllegalArgumentException(
          s"Unknown operation: $operation. Valid operations are: read, write, list, exists"
        )
    }
  }

  private def readFile(path: Path): String = {
    if (!Files.exists(path)) {
      throw new IllegalArgumentException(s"File does not exist: $path")
    }
    if (!Files.isRegularFile(path)) {
      throw new IllegalArgumentException(s"Path is not a file: $path")
    }
    if (!Files.isReadable(path)) {
      throw new IllegalArgumentException(s"File is not readable: $path")
    }

    Files.readString(path)
  }

  private def writeFile(
      path: Path,
      content: String,
      append: Boolean
  ): String = {
    // Create parent directories if they don't exist
    Option(path.getParent).foreach { parent =>
      if (!Files.exists(parent)) {
        Files.createDirectories(parent)
      }
    }

    if (append && Files.exists(path)) {
      Files.writeString(
        path,
        content,
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND
      )
      s"Appended ${content.length} characters to $path"
    } else {
      Files.writeString(path, content)
      s"Wrote ${content.length} characters to $path"
    }
  }

  private def listDirectory(path: Path): String = {
    if (!Files.exists(path)) {
      throw new IllegalArgumentException(s"Directory does not exist: $path")
    }
    if (!Files.isDirectory(path)) {
      throw new IllegalArgumentException(s"Path is not a directory: $path")
    }
    if (!Files.isReadable(path)) {
      throw new IllegalArgumentException(s"Directory is not readable: $path")
    }

    val entries = Files
      .list(path)
      .iterator()
      .asScala
      .map { entry =>
        val name     = entry.getFileName.toString
        val fileType = if (Files.isDirectory(entry)) "dir" else "file"
        val size     = if (Files.isRegularFile(entry)) {
          s" (${Files.size(entry)} bytes)"
        } else ""
        s"[$fileType] $name$size"
      }
      .toList
      .sorted

    if (entries.isEmpty) {
      s"Directory is empty: $path"
    } else {
      s"Contents of $path:\n${entries.mkString("\n")}"
    }
  }

  private def checkExists(path: Path): String = {
    if (Files.exists(path)) {
      val fileType = if (Files.isDirectory(path)) "directory" else "file"
      s"Yes, $path exists (type: $fileType)"
    } else {
      s"No, $path does not exist"
    }
  }
}
