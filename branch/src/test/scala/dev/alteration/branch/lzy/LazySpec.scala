package dev.alteration.branch.lzy

import dev.alteration.branch.macaroni.runtimes.BranchExecutors
import dev.alteration.branch.testkit.fixtures.LoggerFixtureSuite
import munit.FunSuite

import java.io.{PipedInputStream, PipedOutputStream}
import java.time.*
import java.util.logging.Logger
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Try

class LazySpec extends LoggerFixtureSuite {

  given ExecutionContext = BranchExecutors.executionContext

  override def munitValueTransforms: List[ValueTransform] =
    super.munitValueTransforms ++ List(
      new ValueTransform(
        "Lazy",
        { case lzy: Lazy[?] =>
          LazyRuntime.runAsync(lzy)
        }
      )
    )

  test("Lazy.fn") {
    for {
      l <- Lazy.fn("abc")
    } yield assertEquals(l, "abc")
  }

  test("Lazy.flatMap") {
    for {
      l <- Lazy.fn("abc").flatMap(a => Lazy.fn(a + "def"))
    } yield assertEquals(l, "abcdef")
  }

  test("Lazy.map") {
    for {
      l <- Lazy.fn("abc").map(a => a + "def")
    } yield assertEquals(l, "abcdef")
  }

  test("Lazy.flatten") {
    for {
      l <- Lazy.fn(Lazy.fn("abc")).flatten
    } yield assertEquals(l, "abc")
  }

  test("Lazy.zip") {
    for {
      l <- Lazy.fn(42).zip(Lazy.fn("abc"))
    } yield assertEquals(l, (42, "abc"))
  }

  test("Lazy.zipWith") {
    for {
      l <- Lazy.fn(40).zipWith(Lazy.fn(2))(_ + _)
    } yield assertEquals(l, 42)
  }

  test("Lazy.zip with error in first") {
    val result = Lazy
      .fail[Int](new Exception("error"))
      .zip(Lazy.fn("abc"))
      .runSync()

    assert(result.isFailure)
    assertEquals(result.failed.get.getMessage, "error")
  }

  test("Lazy.zip with error in second") {
    val result = Lazy
      .fn(42)
      .zip(Lazy.fail[String](new Exception("error")))
      .runSync()

    assert(result.isFailure)
    assertEquals(result.failed.get.getMessage, "error")
  }

  test("Lazy.race - left completes first") {
    for {
      result <- Lazy.fn(42).race(Lazy.sleep(1.second).as("slow"))
    } yield {
      result match {
        case i: Int    => assertEquals(i, 42)
        case s: String => fail(s"Expected Int but got String: $s")
      }
    }
  }

  test("Lazy.race - right completes first") {
    for {
      result <- Lazy.sleep(1.second).as(42).race(Lazy.fn("fast"))
    } yield {
      result match {
        case i: Int    => fail(s"Expected String but got Int: $i")
        case s: String => assertEquals(s, "fast")
      }
    }
  }

  test("Lazy.race - left fails, right succeeds") {
    for {
      result <- Lazy
                  .fail[Int](new Exception("left error"))
                  .race(Lazy.fn("success"))
    } yield {
      result match {
        case i: Int    => fail(s"Expected String but got Int: $i")
        case s: String => assertEquals(s, "success")
      }
    }
  }

  test("Lazy.race - left succeeds, right fails") {
    for {
      result <- Lazy.fn(42).race(Lazy.fail[String](new Exception("right error")))
    } yield {
      result match {
        case i: Int    => assertEquals(i, 42)
        case s: String => fail(s"Expected Int but got String: $s")
      }
    }
  }

  test("Lazy.race - both fail") {
    val result = Lazy
      .fail[Int](new Exception("left error"))
      .race(Lazy.fail[String](new Exception("right error")))
      .runSync()

    assert(result.isFailure)
    // First failure should be returned
    assert(
      result.failed.get.getMessage == "left error" ||
        result.failed.get.getMessage == "right error"
    )
  }

  test("Lazy.recover") {
    for {
      l <- Lazy.fail(new Exception("error")).recover(_ => Lazy.fn("abc"))
    } yield assertEquals(l, "abc")
  }

  test("Lazy.recoverSome") {
    val a = for {
      _ <- Lazy.fail(new ArithmeticException("error")).recoverSome {
             case _: IllegalArgumentException =>
               Lazy.fn("abc")
           }
    } yield ()

    val aResult = a.runSync()
    assert(aResult.isFailure)
    assert(aResult.failed.get.isInstanceOf[ArithmeticException])

    val b = for {
      l <- Lazy.fail(new ArithmeticException("error")).recoverSome {
             case _: ArithmeticException =>
               Lazy.fn("abc")
           }
    } yield l

    val bResult = b.runSync()
    assert(bResult.isSuccess)
    assertEquals(bResult.get, "abc")
  }

  test("Lazy.orElse") {
    for {
      l <- Lazy.fail(new Exception("error")).orElse(Lazy.fn("abc"))
    } yield assertEquals(l, "abc")
  }

  test("Lazy.orElseValue") {
    for {
      l <- Lazy.fail(new Exception("error")).orElseDefault("abc")
    } yield assertEquals(l, "abc")
  }

  test("Lazy.iterate") {
    for {
      l     <- Lazy.iterate((1 to 1000000).iterator)(List.newBuilder[Int])(_ =>
                 Lazy.fn(1)
               )
      empty <-
        Lazy.iterate(Iterator.empty)(List.newBuilder[Int])(_ => Lazy.fn(1))
    } yield {
      assertEquals(l.size, 1000000)
      assertEquals(l.sum, 1000000)
      assertEquals(empty.size, 0)
    }
  }

  test("Lazy.forEach - Set") {
    for {
      l <- Lazy.forEach(Set(1, 2, 3, 4, 5, 5, 4, 3, 2, 1))(Lazy.fn)
    } yield assertEquals(l.sum, 15)
  }

  test("Lazy.forEach - List") {
    for {
      l <- Lazy.forEach(List(1, 2, 3, 4, 5))(Lazy.fn)
    } yield assertEquals(l.sum, 15)
  }

  test("Lazy.forEach - Seq") {
    for {
      l <- Lazy.forEach(Seq(1, 2, 3, 4, 5))(Lazy.fn)
    } yield assertEquals(l.sum, 15)
  }

  test("Lazy.forEach - IndexedSeq") {
    for {
      l <- Lazy.forEach(IndexedSeq(1, 2, 3, 4, 5))(Lazy.fn)
    } yield assertEquals(l.sum, 15)
  }

  test("Lazy.forEach - Vector") {
    for {
      l <- Lazy.forEach(Vector(1, 2, 3, 4, 5))(Lazy.fn)
    } yield assertEquals(l.sum, 15)
  }

  test("Lazy.forEach - Array") {
    for {
      l <- Lazy.forEach(Array(1, 2, 3, 4, 5))(Lazy.fn)
    } yield assertEquals(l.sum, 15)
  }

  test("Lazy.sequence - List") {
    for {
      result <- Lazy.sequence(List(Lazy.fn(1), Lazy.fn(2), Lazy.fn(3)))
    } yield assertEquals(result, List(1, 2, 3))
  }

  test("Lazy.sequence - Vector") {
    for {
      result <- Lazy.sequence(Vector(Lazy.fn("a"), Lazy.fn("b"), Lazy.fn("c")))
    } yield assertEquals(result, Vector("a", "b", "c"))
  }

  test("Lazy.sequence - Set") {
    for {
      result <- Lazy.sequence(Set(Lazy.fn(1), Lazy.fn(2), Lazy.fn(3)))
    } yield assertEquals(result, Set(1, 2, 3))
  }

  test("Lazy.sequence - Array") {
    for {
      result <- Lazy.sequence(Array(Lazy.fn(1), Lazy.fn(2), Lazy.fn(3)))
    } yield assertEquals(result.toList, List(1, 2, 3))
  }

  test("Lazy.sequence - empty List") {
    for {
      result <- Lazy.sequence(List.empty[Lazy[Int]])
    } yield assertEquals(result, List.empty[Int])
  }

  test("Lazy.sequence - with error") {
    val result = Lazy
      .sequence(List(Lazy.fn(1), Lazy.fail[Int](new Exception("error")), Lazy.fn(3)))
      .runSync()

    assert(result.isFailure)
    assertEquals(result.failed.get.getMessage, "error")
  }

  test("Lazy.traverse - List") {
    for {
      result <- Lazy.traverse(List(1, 2, 3))(x => Lazy.fn(x * 2))
    } yield assertEquals(result, List(2, 4, 6))
  }

  test("Lazy.traverse - Vector") {
    for {
      result <- Lazy.traverse(Vector("a", "b", "c"))(s => Lazy.fn(s.toUpperCase))
    } yield assertEquals(result, Vector("A", "B", "C"))
  }

  test("Lazy.traverse - Set") {
    for {
      result <- Lazy.traverse(Set(1, 2, 3))(x => Lazy.fn(x.toString))
    } yield assertEquals(result, Set("1", "2", "3"))
  }

  test("Lazy.traverse - Array") {
    for {
      result <- Lazy.traverse(Array(1, 2, 3))(x => Lazy.fn(x * 2))
    } yield assertEquals(result.toList, List(2, 4, 6))
  }

  test("Lazy.parSequence - List") {
    for {
      result <- Lazy.parSequence(List(Lazy.fn(1), Lazy.fn(2), Lazy.fn(3)))
    } yield assertEquals(result, List(1, 2, 3))
  }

  test("Lazy.parSequence - Vector") {
    for {
      result <- Lazy.parSequence(
                  Vector(Lazy.fn("a"), Lazy.fn("b"), Lazy.fn("c"))
                )
    } yield assertEquals(result, Vector("a", "b", "c"))
  }

  test("Lazy.parSequence - parallel execution") {
    val start = System.currentTimeMillis()

    val lazies = List(
      Lazy.sleep(100.millis).as(1),
      Lazy.sleep(100.millis).as(2),
      Lazy.sleep(100.millis).as(3)
    )

    for {
      result <- Lazy.parSequence(lazies)
      end = System.currentTimeMillis()
    } yield {
      assertEquals(result, List(1, 2, 3))
      // Should take ~100ms in parallel, not 300ms sequentially
      assert(
        (end - start) < 250,
        s"Took ${end - start}ms, expected < 250ms for parallel execution"
      )
    }
  }

  test("Lazy.parSequence - with error") {
    val result = Lazy
      .parSequence(
        List(Lazy.fn(1), Lazy.fail[Int](new Exception("error")), Lazy.fn(3))
      )
      .runSync()

    assert(result.isFailure)
    assertEquals(result.failed.get.getMessage, "error")
  }

  test("Lazy.parTraverse - List") {
    for {
      result <- Lazy.parTraverse(List(1, 2, 3))(x => Lazy.fn(x * 2))
    } yield assertEquals(result, List(2, 4, 6))
  }

  test("Lazy.parTraverse - Vector") {
    for {
      result <- Lazy.parTraverse(Vector("a", "b", "c"))(s =>
                  Lazy.fn(s.toUpperCase)
                )
    } yield assertEquals(result, Vector("A", "B", "C"))
  }

  test("Lazy.parTraverse - parallel execution") {
    val start = System.currentTimeMillis()

    for {
      result <- Lazy.parTraverse(List(1, 2, 3)) { x =>
                  Lazy.sleep(100.millis).as(x * 2)
                }
      end = System.currentTimeMillis()
    } yield {
      assertEquals(result, List(2, 4, 6))
      // Should take ~100ms in parallel, not 300ms sequentially
      assert(
        (end - start) < 250,
        s"Took ${end - start}ms, expected < 250ms for parallel execution"
      )
    }
  }

  test("Lazy.now") {
    for {
      l <- Lazy.now()
    } yield assert(l.isBefore(Instant.now()))
  }

  test("Lazy.now - adjusted clock") {
    def clockAt(instant: Instant) = Clock.fixed(instant, ZoneId.of("UTC"))
    for {
      now <- Lazy.now()
      a   <- Lazy.now(clockAt(now.plusSeconds(3600)))
    } yield {
      assert(Duration.between(now, a).toHours == 1)
    }
  }

  test("Lazy.retryN") {
    var i = 0
    for {
      l <- Lazy
             .fn {
               i += 1
               if i <= 2 then throw new Exception("error")
               else i
             }
             .retryN(5)
             .recover(_ => Lazy.fn(100))
      f <- Lazy
             .fn {
               i += 1
               if i <= 10 then throw new Exception("error")
               else i
             }
             .retryN(5)
             .recover(_ => Lazy.fn(100))
    } yield {
      assertEquals(l, 3)
      assertEquals(f, 100)
    }
  }

  test("Lazy.sleep") {
    for {
      start <- Lazy.now()
      _     <- Lazy.sleep(1.second)
      end   <- Lazy.now()
    } yield {
      assert(java.time.Duration.between(start, end).getSeconds >= 1)
    }
  }

  test("Lazy.delay") {
    for {
      a <- Lazy.now()
      b <- Lazy.now().delay(1.second)
      c <- Lazy.now()
    } yield {
      assert(java.time.Duration.between(a, b).getSeconds >= 1)
      assert(java.time.Duration.between(b, c).getSeconds < 1)
    }
  }

  test("Lazy.pause") {
    for {
      a <- Lazy.now()
      b <- Lazy.now().pause(1.second)
      c <- Lazy.now()
    } yield {
      assert(java.time.Duration.between(a, b).getSeconds < 1)
      assert(java.time.Duration.between(b, c).getSeconds >= 1)
    }
  }

  test("Lazy.timeout - completes before timeout") {
    for {
      result <- Lazy.fn(42).timeout(2.seconds)
    } yield assertEquals(result, 42)
  }

  test("Lazy.timeout - times out") {
    val result = Lazy
      .sleep(2.seconds)
      .as(42)
      .timeout(500.millis)
      .runSync()

    assert(result.isFailure)
    assert(result.failed.get.isInstanceOf[java.util.concurrent.TimeoutException])
    assert(result.failed.get.getMessage.contains("Operation timed out"))
  }

  test("Lazy.timeout - error before timeout") {
    val result = Lazy
      .fail[Int](new Exception("test error"))
      .timeout(2.seconds)
      .runSync()

    assert(result.isFailure)
    assert(result.failed.get.getMessage == "test error")
  }

  loggerFixture.test("Lazy.log") { (logger, handler) =>
    given Logger = logger

    for {
      _ <- Lazy.logSevere("severe", new Exception("severe"))
      _ <- Lazy.logWarning("warning")
      _ <- Lazy.logInfo("info")
      _ <- Lazy.logConfig("config")
      _ <- Lazy.logFine("fine")
    } yield {
      assertEquals(
        handler.records.map(_.getMessage).toList,
        List("severe", "warning", "info", "config", "fine")
      )
      assert(handler.records.flatMap(r => Option(r.getThrown)).size == 1)
    }
  }

  loggerFixture.test("Lazy.logError") { (logger, handler) =>
    given Logger = logger
    for {
      _ <- Lazy.fail(new Exception("error")).logError.ignore
    } yield {
      assertEquals(handler.records.head.getMessage, "error")
    }
  }

  test("Lazy.tapError") {
    var counter = 0
    for {
      _ <- Lazy.fn(42).tapError(_ => counter += 1).ignore
      _ <- Lazy.fail(new Exception("error")).tapError(_ => counter += 1).ignore
    } yield {
      assertEquals(counter, 1)
    }
  }

  test("Lazy.mapError") {
    val result = Lazy
      .fail(new Exception("error"))
      .mapError(_ => new Exception("mapped error"))
      .runSync()

    assert(result.isFailure)
    assertEquals(result.failed.get.getMessage, "mapped error")

  }

  test("Lazy.either - success") {
    for {
      result <- Lazy.fn(42).either
    } yield {
      assert(result.isRight)
      assertEquals(result.toOption.get, 42)
    }
  }

  test("Lazy.either - failure") {
    for {
      result <- Lazy.fail[Int](new Exception("test error")).either
    } yield {
      assert(result.isLeft)
      assertEquals(result.left.toOption.get.getMessage, "test error")
    }
  }

  test("Lazy.fromTry - captured") {
    // If the Try is not previously evaluated, it will be evaluated on each run
    @volatile
    var counter = 0
    val lzyTry  = Lazy.fromTry(Try(counter += 1))
    assert(counter == 0)
    lzyTry.runSync()
    assert(counter == 1)
    lzyTry.runSync()
    assert(counter == 2)
  }

  test("Lazy.fromTry - by value") {
    // If the Try is already evaluated, it won't be re-evaluated
    @volatile
    var counter    = 0
    val alreadyTry = Try(counter += 1)
    assert(counter == 1)
    val lzyTry     = Lazy.fromTry(alreadyTry)
    assert(counter == 1)
    lzyTry.runSync()
    assert(counter == 1)
  }

  test("Lazy.fromFuture - success") {
    for {
      result <- Lazy.fromFuture(Future.successful(42))
    } yield assertEquals(result, 42)
  }

  test("Lazy.fromFuture - failure") {
    val result = Lazy
      .fromFuture(Future.failed[Int](new Exception("future error")))
      .runSync()

    assert(result.isFailure)
    assertEquals(result.failed.get.getMessage, "future error")
  }

  test("Lazy.fromFuture - with flatMap") {
    for {
      a <- Lazy.fromFuture(Future.successful(20))
      b <- Lazy.fromFuture(Future.successful(22))
    } yield assertEquals(a + b, 42)
  }

  test("Lazy.using") {
    Lazy
      .using(scala.io.Source.fromResource("app-config.json")) { r =>
        r.getLines().mkString
      }
      .map(str => assert(str.nonEmpty))
  }

  test("Lazy.when") {
    for {
      a <- Lazy.when(true)(Lazy.fn(42))
      b <- Lazy.when(false)(Lazy.fn(42))
      c <- Lazy.fn(42).when(true)
      d <- Lazy.fn(42).when(false)
    } yield {
      assert(a.contains(42))
      assert(b.isEmpty)
      assert(c.contains(42))
      assert(d.isEmpty)
    }
  }

  test("Lazy.whenCase") {
    for {
      a <- Lazy.whenCase("yes") {
             case "no"  => Lazy.fn(21)
             case "yes" => Lazy.fn(42)
           }
      b <- Lazy.whenCase("no") { case "yes" =>
             Lazy.fn(42)
           }

    } yield {
      assert(a.contains(42))
      assert(b.isEmpty)
    }
  }

  test("Lazy.fromOption") {
    for {
      a <- Lazy.fromOption(Some(42))
      b <- Lazy.fromOption(Option.empty[Int]).orElseDefault(21)
    } yield {
      assertEquals(a, 42)
      assertEquals(b, 21)
    }
  }

  test("Lazy.optional") {
    for {
      a <- Lazy.fn(42).optional
      b <- Lazy.fail[Int](new Exception("error")).optional
    } yield {
      assert(a.contains(42))
      assert(b.isEmpty)
    }
  }

  test("Lazy.someOrElse") {
    for {
      a <- Lazy
             .fn(Some(42))
             .someOrElse(21)
      b <- Lazy
             .fn(Option.empty[Int])
             .someOrElse(21)
    } yield {
      assertEquals(a, 42)
      assertEquals(b, 21)
    }
  }

  test("Lazy.someOrFail") {
    assert(
      Lazy
        .fn(Some(42))
        .someOrFail(new Exception("error"))
        .runSync()
        .isSuccess
    )

    assert(
      Lazy
        .fn(Option.empty[Int])
        .someOrFail(new Exception("error"))
        .runSync()
        .isFailure
    )

  }

  test("Lazy.usingManager") {
    val expected = "testing 123"
    for {
      managed <- Lazy
                   .usingManager { manager =>
                     val in  = manager(new PipedInputStream())
                     val out = manager(new PipedOutputStream(in))
                     out.write(expected.getBytes)
                     new String(in.readNBytes(expected.length))
                   }
    } yield assert(managed.equals(expected))
  }

  test("Lazy.managed") {
    val expected = "testing 123"
    val managed  = Lazy.usingManager { implicit manager =>
      {
        for {
          in   <- Lazy.managed(new PipedInputStream())
          out  <- Lazy.managed(new PipedOutputStream(in))
          _    <- Lazy.fn(out.write(expected.getBytes))
          read <- Lazy.fn(in.readNBytes(expected.length))
        } yield new String(read)
      }.runSync()
      // If we don't runSync() here,
      // the manager will close the resources before they can be used.
      // This is true for anything "async" with Lazy.usingManager
    }
    managed.map { t =>
      assert(t.get.equals(expected))
    }
  }

  test("Lazy.until result conditional") {

    def rnd = Lazy
      .fn(Math.random() * 10)
      .map(_.toInt)

    for {
      one   <- rnd.until(_ == 1)
      three <- rnd.until(_ == 3)
    } yield {
      assert(one == 1)
      assert(three == 3)
    }
  }

  test("Lazy.until any conditional") {

    @volatile
    var counter = 10

    for {
      result <- Lazy
                  .fn {
                    counter -= 1
                    counter + 1
                  }
                  .until(counter == 0)
    } yield {
      assert(result == 1)
    }
  }

}
