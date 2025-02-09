package dev.wishingtree.branch.friday

import scala.util.*
import java.time.*
import munit.*

class JsonCodecSpec extends FunSuite {
  given instantCodec: JsonCodec[Instant] =
    JsonCodec[String].transform(Instant.parse)(_.toString)

  test("JsonCodec.transform") {
    val instant = Instant.now()
    val json    = instantCodec.encode(instant)
    assertEquals(instantCodec.decode(json), Success(instant))

    case class TimeCapsule(instant: Instant) derives JsonCodec
    val jsStr    = """{"instant":"2024-12-27T03:30:29.460232Z"}"""
    val expected = Instant.parse("2024-12-27T03:30:29.460232Z")
    assertEquals(
      summon[JsonCodec[TimeCapsule]].decode(jsStr),
      Success(TimeCapsule(expected))
    )

  }

}
