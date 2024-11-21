import dev.wishingtree.branch.lzy.{Lazy, LazyApp}

object LazyAppExample extends LazyApp {

  def myEffects: Lazy[Unit] =
    for {
      _ <- Lazy.fn(println("Enter a number"))
      a <- Lazy.fn(scala.io.StdIn.readInt())
      b <- Lazy.fn[Int](scala.util.Random.nextInt(5))
    } yield println(a * b)

  val bigPrint = Lazy.forEach(1 to 10000)(i => Lazy.fn(println(i)))

  val recoveredEffect = {
    lazy val loop: Lazy[Unit] = myEffects.recover(_ => loop)
    loop
  }

  val failApp =
    for {
      _ <- Lazy.println("Here")
      _ <- Lazy.fail(new Exception("the app stops here"))
      _ <- Lazy.println("There")
    } yield ()

  val flattendEffect = Lazy
    .fn(
      Lazy.fn("42").debug("inner")
    )
    .flatten
    .debug("outter")

  override def run: Lazy[Any] =
    flattendEffect.debug("The answer")
}
