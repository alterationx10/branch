package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.testkit.fixtures.FileFixtureSuite
import java.nio.file.Files

class SafeFileSystemExecutorSpec extends FileFixtureSuite {

  tmpDir.test("FileSystemPolicy should allow operations within sandbox") { dir =>
    val policy   = FileSystemPolicy(sandboxPath = Some(dir))
    val registry = ToolRegistry().registerFileSystem(policy)

    // Create a file within sandbox
    val testFile = dir.resolve("test.txt")
    Files.writeString(testFile, "Hello")

    import dev.alteration.branch.friday.Json._
    val args = JsonObject(
      Map(
        "operation" -> JsonString("read"),
        "path" -> JsonString(testFile.toString)
      )
    )

    val result = registry.execute(
      "dev.alteration.branch.hollywood.tools.provided.FileSystemTool",
      args
    )

    result match {
      case Some(JsonString(content)) =>
        assertEquals(content, "Hello", "Should read file content")
      case _                         => fail("Expected successful read")
    }
  }

  tmpDir.test("FileSystemPolicy should block operations outside sandbox") {
    dir =>
      val policy   = FileSystemPolicy(sandboxPath = Some(dir))
      val registry = ToolRegistry().registerFileSystem(policy)

      import dev.alteration.branch.friday.Json._
      val args = JsonObject(
        Map(
          "operation" -> JsonString("read"),
          "path" -> JsonString("/etc/passwd") // Outside sandbox
        )
      )

      val result = registry.execute(
        "dev.alteration.branch.hollywood.tools.provided.FileSystemTool",
        args
      )

      result match {
        case Some(JsonString(error)) =>
          assert(
            error.contains("outside sandbox"),
            s"Should block access outside sandbox. Got: $error"
          )
        case _                       => fail("Expected security error")
      }
  }

  tmpDir.test("FileSystemPolicy should block write operations when read-only") {
    dir =>
      val policy   = FileSystemPolicy(sandboxPath = Some(dir), readOnly = true)
      val registry = ToolRegistry().registerFileSystem(policy)

      import dev.alteration.branch.friday.Json._
      val args = JsonObject(
        Map(
          "operation" -> JsonString("write"),
          "path" -> JsonString(dir.resolve("test.txt").toString),
          "content" -> JsonString("test")
        )
      )

      val result = registry.execute(
        "dev.alteration.branch.hollywood.tools.provided.FileSystemTool",
        args
      )

      result match {
        case Some(JsonString(error)) =>
          assert(
            error.contains("read-only") || error.contains("disabled"),
            s"Should block write in read-only mode. Got: $error"
          )
        case _                       => fail("Expected security error")
      }
  }

  tmpDir.test("FileSystemPolicy should block paths with blocked patterns") {
    dir =>
      val policy = FileSystemPolicy(
        sandboxPath = Some(dir),
        blockedPatterns = List(".env", ".key")
      )
      val registry = ToolRegistry().registerFileSystem(policy)

      import dev.alteration.branch.friday.Json._
      val args = JsonObject(
        Map(
          "operation" -> JsonString("read"),
          "path" -> JsonString(dir.resolve(".env").toString)
        )
      )

      val result = registry.execute(
        "dev.alteration.branch.hollywood.tools.provided.FileSystemTool",
        args
      )

      result match {
        case Some(JsonString(error)) =>
          assert(
            error.contains("blocked pattern"),
            s"Should block .env file. Got: $error"
          )
        case _                       => fail("Expected security error")
      }
  }

  tmpDir.test("FileSystemPolicy should enforce max file size on writes") {
    dir =>
      val policy = FileSystemPolicy(
        sandboxPath = Some(dir),
        maxFileSize = 100 // 100 bytes
      )
      val registry = ToolRegistry().registerFileSystem(policy)

      import dev.alteration.branch.friday.Json._
      val largeContent = "x" * 200 // 200 bytes
      val args         = JsonObject(
        Map(
          "operation" -> JsonString("write"),
          "path" -> JsonString(dir.resolve("large.txt").toString),
          "content" -> JsonString(largeContent)
        )
      )

      val result = registry.execute(
        "dev.alteration.branch.hollywood.tools.provided.FileSystemTool",
        args
      )

      result match {
        case Some(JsonString(error)) =>
          assert(
            error.contains("exceeds maximum"),
            s"Should block large file write. Got: $error"
          )
        case _                       => fail("Expected security error")
      }
  }

  tmpDir.test("FileSystemPolicy.strict should be read-only and sandboxed") {
    dir =>
      val policy   = FileSystemPolicy.strict(dir)
      val registry = ToolRegistry().registerFileSystem(policy)

      assertEquals(policy.readOnly, true, "Strict policy should be read-only")
      assertEquals(
        policy.sandboxPath,
        Some(dir),
        "Strict policy should have sandbox"
      )
  }

  tmpDir.test("FileSystemPolicy.permissive should allow writes") { dir =>
    val policy = FileSystemPolicy.permissive(Some(dir))

    assertEquals(
      policy.readOnly,
      false,
      "Permissive policy should allow writes"
    )
    assert(
      policy.maxFileSize > 10 * 1024 * 1024,
      "Permissive policy should have larger file size limit"
    )
  }
}
