package examples.friday.typeclass

import dev.alteration.branch.friday.{JsonEncoder, JsonDecoder}
import scala.util.{Success, Failure}

/** Example demonstrating JSON Encoders and Decoders with Friday.
  *
  * This example shows:
  *   - Auto-derivation for case classes using `derives JsonEncoder` and `derives
  *     JsonDecoder`
  *   - Encoding Scala objects to JSON
  *   - Decoding JSON strings to Scala types
  *   - Working with nested case classes
  *   - Handling sum types (enums)
  *   - Error handling with Try
  *
  * To run this example: sbt "examples/runMain
  * examples.friday.typeclass.EncoderDecoderExample"
  */
object EncoderDecoderExample {

  // Product types (case classes) with auto-derivation
  case class Address(street: String, city: String, zipCode: String)
      derives JsonEncoder,
        JsonDecoder

  case class Person(
      name: String,
      age: Int,
      email: String,
      address: Address
  ) derives JsonEncoder,
        JsonDecoder

  case class Account(
      id: Long,
      owner: Person,
      status: String,
      balance: Double
  ) derives JsonEncoder,
        JsonDecoder

  def main(args: Array[String]): Unit = {

    println("=== Friday Encoder/Decoder Example ===\n")

    // Create some data
    val address = Address("123 Main St", "Springfield", "12345")
    val person = Person("Alice", 30, "alice@example.com", address)

    println("=== Encoding (Scala → JSON) ===")

    // Encoding using explicit encoder
    val personEncoder = JsonEncoder.derived[Person]
    val personJson = personEncoder.encode(person)
    println(s"Person as Json AST:\n$personJson\n")

    val personJsonString = personJson.toJsonString
    println(s"Person as JSON string:\n$personJsonString\n")

    // Encoding a complex object
    val account = Account(
      id = 10001L,
      owner = person,
      status = "active",
      balance = 1234.56
    )
    val accountEncoder = JsonEncoder.derived[Account]

    println(s"Account as JSON:\n${accountEncoder.encode(account).toJsonString}\n")

    println("=== Decoding (JSON → Scala) ===")

    // Decoding a simple case class
    val addressJsonStr = """{"street":"456 Oak Ave","city":"Metropolis","zipCode":"54321"}"""
    val addressDecoder = JsonDecoder.derived[Address]

    addressDecoder.decode(addressJsonStr) match {
      case Success(addr) =>
        println(s"✓ Decoded address: ${addr.street}, ${addr.city}")
      case Failure(ex) =>
        println(s"✗ Failed to decode address: ${ex.getMessage}")
    }

    // Decoding a nested case class
    val personJsonStr =
      """{"name":"Bob","age":42,"email":"bob@example.com","address":{"street":"789 Elm St","city":"Gotham","zipCode":"99999"}}"""
    val personDecoder = JsonDecoder.derived[Person]

    personDecoder.decode(personJsonStr) match {
      case Success(p) =>
        println(s"✓ Decoded person: ${p.name}, age ${p.age}, lives in ${p.address.city}")
      case Failure(ex) =>
        println(s"✗ Failed to decode person: ${ex.getMessage}")
    }

    // Decoding a full account
    val accountJsonStr =
      """{"id":20002,"owner":{"name":"Charlie","age":35,"email":"charlie@example.com","address":{"street":"321 Pine Rd","city":"Star City","zipCode":"11111"}},"status":"active","balance":9876.54}"""
    val accountDecoder = JsonDecoder.derived[Account]

    accountDecoder.decode(accountJsonStr) match {
      case Success(acc) =>
        println(
          s"✓ Decoded account: ID ${acc.id}, owner ${acc.owner.name}, balance $$${acc.balance}"
        )
      case Failure(ex) =>
        println(s"✗ Failed to decode account: ${ex.getMessage}")
    }

    println()

    // Error handling
    println("=== Error Handling ===")
    val invalidJson = """{"name":"Dave","age":"not-a-number"}"""

    personDecoder.decode(invalidJson) match {
      case Success(p) =>
        println(s"Decoded: $p")
      case Failure(ex) =>
        println(s"✗ Expected error - invalid age field: ${ex.getMessage}")
    }

    val missingFields = """{"name":"Eve"}"""
    personDecoder.decode(missingFields) match {
      case Success(p) =>
        println(s"Decoded: $p")
      case Failure(ex) =>
        println(s"✗ Expected error - missing required fields: ${ex.getMessage}")
    }

    println("\n=== Example Complete ===")
  }

}
