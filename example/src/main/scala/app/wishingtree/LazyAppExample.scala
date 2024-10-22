package app.wishingtree

import dev.wishingtree.branch.lzy.{Lazy, LazyApp}

object LazyAppExample extends LazyApp{

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

  override def run: Lazy[Any] =
    bigPrint.flatMap(_ => recoveredEffect.forever)
}
