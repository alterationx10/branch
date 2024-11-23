import dev.wishingtree.branch.lzy.*

val prnt = Lazy.fn {
  println("Hello, World!")
}

prnt.runSync()