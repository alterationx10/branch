//> using target.scope test
package dev.wishingtree.branch.testkit.fixtures

import java.nio.file._
import munit.*

class FileFixtureSuite extends FunSuite {

  val files = FunFixture[Path](
    setup = { test =>
      Files.createTempFile("tmp", test.name)
    },
    teardown = { file =>
      Files.deleteIfExists(file)
    }
  )

  def filesWithContent(content: String) = FunFixture[Path](
    setup = { test =>
      val path = Files.createTempFile("tmp", test.name)
      Files.writeString(path, content)
      path
    },
    teardown = { file =>
      Files.deleteIfExists(file)
    }
  )

}
