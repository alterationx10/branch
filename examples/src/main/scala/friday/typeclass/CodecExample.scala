package examples.friday.typeclass

import dev.alteration.branch.friday.JsonCodec
import scala.util.{Success, Failure}
import java.time.Instant

/** Example demonstrating JSON Codecs with Friday.
  *
  * A JsonCodec combines both encoding and decoding capabilities. This example shows:
  *   - Auto-derivation for case classes using `derives JsonCodec`
  *   - Using codec extension methods (toJson, toJsonString, decodeAs)
  *   - Transforming codecs to work with different types
  *   - Creating custom codecs for types like Instant
  *   - Round-trip encoding/decoding
  *
  * To run this example: sbt "examples/runMain examples.friday.typeclass.CodecExample"
  */
object CodecExample {

  // Create a custom codec for Instant by transforming the String codec
  // This must be defined before any case classes that use Instant
  given JsonCodec[Instant] = JsonCodec[String].transform(Instant.parse)(_.toString)

  // Simple case class with codec derivation
  case class Item(id: Long, name: String, price: Double, inStock: Boolean)
      derives JsonCodec

  // Nested case classes
  case class Coordinates(lat: Double, lon: Double) derives JsonCodec

  case class Location(name: String, coordinates: Coordinates) derives JsonCodec

  case class Order(
      id: String,
      item: Item,
      quantity: Int,
      status: String
  ) derives JsonCodec

  def main(args: Array[String]): Unit = {

    println("=== Friday Codec Example ===\n")

    println("=== Basic Codec Usage ===")

    val item = Item(
      id = 12345L,
      name = "Wireless Mouse",
      price = 29.99,
      inStock = true
    )

    // Encoding
    val itemCodec = JsonCodec[Item]
    val itemJson = itemCodec.encode(item)
    val itemJsonStr = itemJson.toJsonString

    println(s"Item as JSON:\n$itemJsonStr\n")

    // Decoding
    JsonCodec[Item].decode(itemJsonStr) match {
      case Success(p) =>
        println(s"✓ Round-trip successful: ${p.name} costs $$${p.price}")
      case Failure(ex) =>
        println(s"✗ Decoding failed: ${ex.getMessage}")
    }

    println()

    println("=== Working with Nested Structures ===")

    val location = Location(
      name = "San Francisco Office",
      coordinates = Coordinates(37.7749, -122.4194)
    )

    val locationJsonStr = JsonCodec[Location].encode(location).toJsonString
    println(s"Location as JSON:\n$locationJsonStr\n")

    JsonCodec[Location].decode(locationJsonStr) match {
      case Success(loc) =>
        println(
          s"✓ Decoded location: ${loc.name} at (${loc.coordinates.lat}, ${loc.coordinates.lon})"
        )
      case Failure(ex) =>
        println(s"✗ Failed: ${ex.getMessage}")
    }

    println()

    println("=== Working with Nested Case Classes ===")

    val orders = List(
      Order(
        id = "ORD-001",
        item = item,
        quantity = 2,
        status = "pending"
      ),
      Order(
        id = "ORD-002",
        item = Item(67890L, "Keyboard", 79.99, true),
        quantity = 1,
        status = "shipped"
      ),
      Order(
        id = "ORD-003",
        item = Item(11111L, "Monitor", 299.99, false),
        quantity = 1,
        status = "cancelled"
      )
    )

    orders.foreach { order =>
      val orderJson = JsonCodec[Order].encode(order).toJsonString
      println(s"Order ${order.id}:")
      println(s"  Status: ${order.status}")
      println(s"  JSON: $orderJson")

      // Round-trip test
      JsonCodec[Order].decode(orderJson) match {
        case Success(decoded) =>
          println(s"  ✓ Round-trip: ${decoded.id} - ${decoded.status}")
        case Failure(ex) =>
          println(s"  ✗ Failed to decode: ${ex.getMessage}")
      }
      println()
    }

    println("=== Codec Transformations ===")

    // Transform a codec using bimap
    case class UserId(value: Long)
    given JsonCodec[UserId] = JsonCodec[Long].bimap(UserId.apply)(_.value)

    val userId = UserId(42L)
    val userIdJson = summon[JsonCodec[UserId]].encode(userId).toJsonString
    println(s"UserId as JSON: $userIdJson")

    JsonCodec[UserId].decode("99") match {
      case Success(id) =>
        println(s"✓ Decoded UserId: ${id.value}")
      case Failure(ex) =>
        println(s"✗ Failed: ${ex.getMessage}")
    }

    println()

    // Transform with map
    case class Email(value: String)
    given JsonCodec[Email] = JsonCodec[String]
      .map(s => Email(s.toLowerCase))(_.value)

    val email = Email("USER@EXAMPLE.COM")
    val emailJson = summon[JsonCodec[Email]].encode(email).toJsonString
    println(s"Email as JSON: $emailJson")

    JsonCodec[Email].decode("\"ANOTHER@EXAMPLE.COM\"") match {
      case Success(e) =>
        println(s"✓ Decoded and normalized email: ${e.value}")
      case Failure(ex) =>
        println(s"✗ Failed: ${ex.getMessage}")
    }

    println()

    println("=== Working with Collections ===")

    // The given codecs handle collections automatically
    val itemList = List(
      Item(1L, "Widget A", 10.0, true),
      Item(2L, "Widget B", 20.0, false),
      Item(3L, "Widget C", 30.0, true)
    )

    val listJson = JsonCodec[List[Item]].encode(itemList).toJsonString
    println(s"Item list as JSON:\n$listJson\n")

    JsonCodec[List[Item]].decode(listJson) match {
      case Success(items) =>
        println(s"✓ Decoded ${items.length} items:")
        items.foreach(i => println(s"  - ${i.name}: $$${i.price}"))
      case Failure(ex) =>
        println(s"✗ Failed: ${ex.getMessage}")
    }

    println("\n=== Example Complete ===")
  }

}
