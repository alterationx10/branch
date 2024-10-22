package dev.wishingtree.branch.lzy

trait LazyApp {
  def run: Lazy[Any]

  final def main(args: Array[String]): Unit =
    LazyRuntime.runSync(run)
}
