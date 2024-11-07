package app.wishingtree

import com.sun.net.httpserver.Filter
import dev.wishingtree.branch.spider.Paths.*
import dev.wishingtree.branch.spider.*

object HttpAppExample extends HttpApp {

  import RequestHandler.given

  val staticFilesPath =  >> / java.nio.file.Path.of("").toAbsolutePath.toString / "site" / "book"
  val files = FileContext(staticFilesPath)

  ContextHandler.registerHandler(files)
}
