import dev.wishingtree.branch.lzy.{Lazy, LazyRuntime}
import munit.FunSuite

import java.time.*
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class LazySpec extends FunSuite {

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

  test("Lazy.forEach") {
    for {
      l <- Lazy.forEach(1 to 10000)(_ => Lazy.fn(1))
    } yield assertEquals(l.sum, 10000)
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
}
