package dev.wishingtree.branch.lzy

import dev.wishingtree.branch.lzy.{Lazy, LazyRuntime}
import dev.wishingtree.branch.macaroni.runtimes.BranchExecutors
import dev.wishingtree.branch.testkit.fixtures.LoggerFixtureSuite
import munit.FunSuite

import java.time.*
import java.util.logging.Logger
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Try

class LazySpec extends LoggerFixtureSuite {

  given ExecutionContext = BranchExecutors.executionContext

  override def munitValueTransforms = super.munitValueTransforms ++ List(
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

  test("Lazy.recover") {
    for {
      l <- Lazy.fail(new Exception("error")).recover(_ => Lazy.fn("abc"))
    } yield assertEquals(l, "abc")
  }

  test("Lazy.recoverSome") {
    val a = for {
      l <- Lazy.fail(new ArithmeticException("error")).recoverSome {
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

  loggerFixture.test("Lazy.tapError") { (logger, handler) =>
    given Logger = logger
    var counter  = 0
    for {
      _ <- Lazy.fn(42).tapError(e => counter += 1).ignore
      _ <- Lazy.fail(new Exception("error")).tapError(e => counter += 1).ignore
    } yield {
      assertEquals(counter, 1)
    }
  }

  test("Lazy.mapError") {
    val result = Lazy
      .fail(new Exception("error"))
      .mapError(e => new Exception("mapped error"))
      .runSync()

    assert(result.isFailure)
    assertEquals(result.failed.get.getMessage, "mapped error")

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

}
