//> using target.scope test
package dev.wishingtree.branch.testkit.fixtures

import java.nio.file.*
import munit.*

import java.util.Comparator

class FileFixtureSuite extends FunSuite {

  val files = FunFixture[Path](
    setup = { test =>
      Files.createTempFile("tmp", test.name)
    },
    teardown = { file =>
      Files.deleteIfExists(file)
    }
  )

  val tmpDir = FunFixture[Path](
    setup = { test =>
      Files.createTempDirectory("tmp")
    },
    teardown = { dir =>
      Files
        .walk(dir)
        .sorted(Comparator.reverseOrder()) // Files before Dirs
        .forEach(Files.deleteIfExists(_))
      Files.deleteIfExists(dir)
    }
  )

  def fileWithSuffix(suffix: String): FunFixture[Path] = FunFixture[Path](
    setup = { test =>
      Files.createTempFile("tmp", test.name + suffix)
    },
    teardown = { file =>
      Files.deleteIfExists(file)
    }
  )

  def fileWithContent(content: String, suffix: String = ""): FunFixture[Path] =
    FunFixture[Path](
      setup = { test =>
        val path = Files.createTempFile("tmp", test.name + suffix)
        Files.writeString(path, content)
        path
      },
      teardown = { file =>
        Files.deleteIfExists(file)
      }
    )

}
