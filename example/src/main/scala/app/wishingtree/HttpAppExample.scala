package app.wishingtree

import com.sun.net.httpserver.Filter
import dev.wishingtree.branch.spider.OpaqueSegments.*
import dev.wishingtree.branch.spider.*

object HttpAppExample extends HttpApp {


  val staticFilesPath =  Segments.wd / "site" / "book"
  val files = FileContext(staticFilesPath)

  ContextHandler.registerHandler(files)
}
