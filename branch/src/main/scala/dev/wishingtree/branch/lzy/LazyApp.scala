package dev.wishingtree.branch.lzy

/** A trait for creating a lazy application with a default main method.
  */
trait LazyApp {

  /** The main Lazy chain to run the application.
    */
  def run: Lazy[Any]

  /** The main method to run the application.
    */
  final def main(args: Array[String]): Unit =
    LazyRuntime.runSync(run)
}
