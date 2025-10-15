package ursula.filetools

import dev.alteration.branch.ursula.UrsulaApp
import dev.alteration.branch.ursula.args.Flags
import dev.alteration.branch.ursula.command.{Command, CommandContext}

import java.nio.file.{Files, Path}

/** Practical file processing CLI tool example.
  *
  * Demonstrates:
  * - Path flags with custom parsers
  * - Custom flag types
  * - Real-world file operations
  * - Options validation
  *
  * Run with:
  * {{{
  * sbt "examples/runMain ursula.filetools.FileToolsExample list -d /tmp"
  * sbt "examples/runMain ursula.filetools.FileToolsExample count -d /tmp --type scala"
  * sbt "examples/runMain ursula.filetools.FileToolsExample find -d /tmp -p "*.txt" --verbose"
  * }}}
  */
object FileToolsExample extends UrsulaApp {
  override val commands: Seq[Command] = Seq(
    ListCommand,
    CountCommand,
    FindCommand
  )
}

object ListCommand extends Command {
  val DirFlag = Flags.path(
    name = "dir",
    shortKey = "d",
    description = "Directory to list",
    default = Some(Path.of(".")),
    parser = s => Path.of(s).toAbsolutePath
  )

  val RecursiveFlag = Flags.boolean(
    name = "recursive",
    shortKey = "r",
    description = "List recursively"
  )

  override val trigger = "list"
  override val description = "List files in a directory"
  override val usage = "list -d <directory> [options]"
  override val examples = Seq(
    "list",
    "list -d /tmp",
    "list -d /tmp --recursive"
  )
  override val flags = Seq(DirFlag, RecursiveFlag)
  override val arguments = Seq.empty

  override def actionWithContext(ctx: CommandContext): Unit = {
    val dir = ctx.requiredFlag(DirFlag)
    val recursive = ctx.booleanFlag(RecursiveFlag)

    if (!Files.exists(dir)) {
      println(s"Error: Directory does not exist: $dir")
      return
    }

    if (!Files.isDirectory(dir)) {
      println(s"Error: Not a directory: $dir")
      return
    }

    println(s"Listing: $dir${if (recursive) " (recursive)" else ""}")
    println()

    if (recursive) {
      Files
        .walk(dir)
        .filter(p => !p.equals(dir))
        .forEach { path =>
          val relative = dir.relativize(path)
          val prefix = if (Files.isDirectory(path)) "[DIR]" else "[FILE]"
          println(s"  $prefix $relative")
        }
    } else {
      Files
        .list(dir)
        .forEach { path =>
          val prefix = if (Files.isDirectory(path)) "[DIR]" else "[FILE]"
          println(s"  $prefix ${path.getFileName}")
        }
    }
  }
}

object CountCommand extends Command {
  // Custom enum for file types
  enum FileType {
    case Scala, Java, All
  }

  val DirFlag = Flags.path(
    name = "dir",
    shortKey = "d",
    description = "Directory to count files in",
    default = Some(Path.of("."))
  )

  val TypeFlag = Flags.custom[FileType](
    name = "type",
    shortKey = "t",
    description = "File type to count (scala, java, all)",
    parser = {
      case "scala" => FileType.Scala
      case "java"  => FileType.Java
      case "all"   => FileType.All
      case other   => throw new IllegalArgumentException(
          s"Unknown file type: $other. Valid options: scala, java, all"
        )
    },
    default = Some(FileType.All),
    options = Some(Set(FileType.Scala, FileType.Java, FileType.All))
  )

  override val trigger = "count"
  override val description = "Count files by type"
  override val usage = "count -d <directory> [--type <type>]"
  override val examples = Seq(
    "count",
    "count -d /tmp",
    "count -d src --type scala"
  )
  override val flags = Seq(DirFlag, TypeFlag)
  override val arguments = Seq.empty

  override def actionWithContext(ctx: CommandContext): Unit = {
    val dir = ctx.requiredFlag(DirFlag)
    val fileType = ctx.requiredFlag(TypeFlag)

    if (!Files.exists(dir) || !Files.isDirectory(dir)) {
      println(s"Error: Invalid directory: $dir")
      return
    }

    val extension = fileType match {
      case FileType.Scala => ".scala"
      case FileType.Java  => ".java"
      case FileType.All   => null
    }

    val count = Files
      .walk(dir)
      .filter(Files.isRegularFile(_))
      .filter { path =>
        extension == null || path.toString.endsWith(extension)
      }
      .count()

    val typeDesc = fileType match {
      case FileType.All => "files"
      case other        => s"${other.toString.toLowerCase} files"
    }

    println(s"Found $count $typeDesc in $dir")
  }
}

object FindCommand extends Command {
  val DirFlag = Flags.path(
    name = "dir",
    shortKey = "d",
    description = "Directory to search",
    default = Some(Path.of("."))
  )

  val PatternFlag = Flags.string(
    name = "pattern",
    shortKey = "p",
    description = "Filename pattern to match",
    required = true
  )

  val VerboseFlag = Flags.boolean(
    name = "verbose",
    shortKey = "v",
    description = "Show file sizes"
  )

  override val trigger = "find"
  override val description = "Find files matching a pattern"
  override val usage = "find -d <directory> -p <pattern>"
  override val examples = Seq(
    "find -p *.scala",
    "find -d src -p Test*.scala",
    "find -p *.java --verbose"
  )
  override val flags = Seq(DirFlag, PatternFlag, VerboseFlag)
  override val arguments = Seq.empty

  override def actionWithContext(ctx: CommandContext): Unit = {
    val dir = ctx.requiredFlag(DirFlag)
    val pattern = ctx.requiredFlag(PatternFlag)
    val verbose = ctx.booleanFlag(VerboseFlag)

    if (!Files.exists(dir) || !Files.isDirectory(dir)) {
      println(s"Error: Invalid directory: $dir")
      return
    }

    // Convert glob pattern to regex
    val regex = pattern
      .replace(".", "\\.")
      .replace("*", ".*")
      .r

    println(s"Searching for '$pattern' in $dir")
    println()

    var count = 0
    Files
      .walk(dir)
      .filter(Files.isRegularFile(_))
      .filter { path =>
        regex.matches(path.getFileName.toString)
      }
      .forEach { path =>
        count += 1
        val relative = dir.relativize(path)
        if (verbose) {
          val size = Files.size(path)
          val sizeStr = if (size < 1024) {
            s"$size B"
          } else if (size < 1024 * 1024) {
            s"${size / 1024} KB"
          } else {
            s"${size / (1024 * 1024)} MB"
          }
          println(s"  $relative ($sizeStr)")
        } else {
          println(s"  $relative")
        }
      }

    println()
    println(s"Found $count file(s)")
  }
}
