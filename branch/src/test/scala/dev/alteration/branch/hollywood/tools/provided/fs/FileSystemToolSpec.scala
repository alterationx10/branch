package dev.alteration.branch.hollywood.tools.provided.fs

import dev.alteration.branch.hollywood.tools.ToolRegistry
import dev.alteration.branch.hollywood.tools.provided.fs.FileSystemTool
import dev.alteration.branch.testkit.fixtures.FileFixtureSuite

import java.nio.file.Files

class FileSystemToolSpec extends FileFixtureSuite {

  fileWithContent("Hello, World!").test("FileSystemTool should read a file") {
    testFile =>
      val tool   = FileSystemTool("read", testFile.toString)
      val result = tool.execute()
      assert(result.isSuccess, "Read operation should succeed")
      result.foreach { content =>
        assertEquals(content, "Hello, World!", "Content should match")
      }
  }

  tmpDir.test("FileSystemTool should write to a new file") { dir =>
    val newFile = dir.resolve("new-file.txt")
    val tool    = FileSystemTool(
      "write",
      newFile.toString,
      content = Some("Test content")
    )
    val result  = tool.execute()

    assert(result.isSuccess, "Write operation should succeed")
    assert(Files.exists(newFile), "File should be created")
    assertEquals(
      Files.readString(newFile),
      "Test content",
      "File content should match"
    )
  }

  fileWithContent("Hello, World!").test(
    "FileSystemTool should overwrite an existing file"
  ) { testFile =>
    val tool   = FileSystemTool(
      "write",
      testFile.toString,
      content = Some("New content")
    )
    val result = tool.execute()

    assert(result.isSuccess, "Write operation should succeed")
    assertEquals(
      Files.readString(testFile),
      "New content",
      "File content should be overwritten"
    )
  }

  fileWithContent("Hello, World!").test(
    "FileSystemTool should append to an existing file"
  ) { testFile =>
    val tool   = FileSystemTool(
      "write",
      testFile.toString,
      content = Some(" Appended text"),
      append = Some(true)
    )
    val result = tool.execute()

    assert(result.isSuccess, "Append operation should succeed")
    assertEquals(
      Files.readString(testFile),
      "Hello, World! Appended text",
      "File content should be appended"
    )
  }

  tmpDir.test("FileSystemTool should list directory contents") { dir =>
    Files.createFile(dir.resolve("file1.txt"))
    Files.createFile(dir.resolve("file2.txt"))
    Files.createDirectory(dir.resolve("subdir"))

    val tool   = FileSystemTool("list", dir.toString)
    val result = tool.execute()

    assert(result.isSuccess, "List operation should succeed")
    result.foreach { content =>
      assert(content.contains("file1.txt"), "Should list file1.txt")
      assert(content.contains("file2.txt"), "Should list file2.txt")
      assert(content.contains("subdir"), "Should list subdir")
      assert(content.contains("[dir]"), "Should mark directory")
      assert(content.contains("[file]"), "Should mark file")
    }
  }

  files.test("FileSystemTool should check if file exists") { testFile =>
    val tool   = FileSystemTool("exists", testFile.toString)
    val result = tool.execute()

    assert(result.isSuccess, "Exists operation should succeed")
    result.foreach { content =>
      assert(content.contains("Yes"), "Should confirm file exists")
    }
  }

  test("FileSystemTool should check if non-existent file exists") {
    val tool   = FileSystemTool("exists", "/nonexistent/file.txt")
    val result = tool.execute()

    assert(result.isSuccess, "Exists operation should succeed")
    result.foreach { content =>
      assert(content.contains("No"), "Should confirm file does not exist")
    }
  }

  test("FileSystemTool should fail to read non-existent file") {
    val tool   = FileSystemTool("read", "/nonexistent/file.txt")
    val result = tool.execute()

    assert(result.isFailure, "Should fail to read non-existent file")
  }

  files.test("FileSystemTool should fail with invalid operation") { testFile =>
    val tool   = FileSystemTool("invalid", testFile.toString)
    val result = tool.execute()

    assert(result.isFailure, "Should fail with invalid operation")
  }

  files.test("FileSystemTool should fail write without content") { testFile =>
    val tool   = FileSystemTool("write", testFile.toString)
    val result = tool.execute()

    assert(
      result.isFailure,
      "Should fail write operation without content"
    )
  }

  tmpDir.test("FileSystemTool should create parent directories when writing") {
    dir =>
      val newFile = dir.resolve("nested/dirs/file.txt")
      val tool    = FileSystemTool(
        "write",
        newFile.toString,
        content = Some("Test content")
      )
      val result  = tool.execute()

      assert(result.isSuccess, "Should create parent directories")
      assert(Files.exists(newFile), "File should be created")
      assert(
        Files.exists(newFile.getParent),
        "Parent directory should be created"
      )
  }

  test("FileSystemTool should register with ToolRegistry") {
    val registry = ToolRegistry()
    registry.register[FileSystemTool]

    val tools = registry.getRegisteredToolNames
    assert(
      tools.contains(
        "dev.alteration.branch.hollywood.tools.provided.fs.FileSystemTool"
      ),
      s"Registry should contain FileSystemTool. Got: $tools"
    )
  }

  files.test("FileSystemTool should execute via ToolRegistry") { testFile =>
    import dev.alteration.branch.friday.Json.*

    val registry = ToolRegistry()
    registry.register[FileSystemTool]

    val args = JsonObject(
      Map(
        "operation" -> JsonString("exists"),
        "path"      -> JsonString(testFile.toString)
      )
    )

    val result = registry.execute(
      "dev.alteration.branch.hollywood.tools.provided.fs.FileSystemTool",
      args
    )
    assert(result.isDefined, "Execution should return a result")
    result.foreach {
      case JsonString(value) =>
        assert(value.contains("Yes"), "Should confirm file exists")
      case _                 => fail("Expected JsonString result")
    }
  }
}
