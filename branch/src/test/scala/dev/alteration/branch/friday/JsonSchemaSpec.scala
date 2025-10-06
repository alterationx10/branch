package dev.alteration.branch.friday

import munit.*
import Json.*

class JsonSchemaSpec extends FunSuite {

  test("derive schema for simple case class") {
    case class Person(name: String, age: Int) derives JsonSchema

    val schema = JsonSchema.of[Person]

    schema match {
      case Schema.ObjectSchema(properties, required, _) =>
        assertEquals(properties.size, 2)
        assert(properties.contains("name"))
        assert(properties.contains("age"))
        assertEquals(required, List("name", "age"))

        properties("name") match {
          case Schema.StringSchema(_) => // expected
          case other => fail(s"Expected StringSchema, got $other")
        }

        properties("age") match {
          case Schema.IntegerSchema(_) => // expected
          case other => fail(s"Expected IntegerSchema, got $other")
        }
      case other => fail(s"Expected ObjectSchema, got $other")
    }
  }

  test("derive schema for case class with Option fields") {
    case class User(name: String, email: Option[String], age: Int) derives JsonSchema

    val schema = JsonSchema.of[User]

    schema match {
      case Schema.ObjectSchema(properties, required, _) =>
        assertEquals(properties.size, 3)

        // Option fields should unwrap to their inner schema
        properties("email") match {
          case Schema.StringSchema(_) => // expected
          case other => fail(s"Expected StringSchema for email, got $other")
        }

        // Option fields should NOT be in the required list
        assertEquals(required, List("name", "age"))
        assertEquals(required.contains("email"), false)
      case other => fail(s"Expected ObjectSchema, got $other")
    }
  }

  test("derive schema with various primitive types") {
    case class AllTypes(
      str: String,
      int: Int,
      long: Long,
      double: Double,
      float: Float,
      bool: Boolean,
      bigDec: BigDecimal
    ) derives JsonSchema

    val schema = JsonSchema.of[AllTypes]

    schema match {
      case Schema.ObjectSchema(properties, _, _) =>
        properties("str") match {
          case Schema.StringSchema(_) => // expected
          case other => fail(s"str: expected StringSchema, got $other")
        }

        properties("int") match {
          case Schema.IntegerSchema(_) => // expected
          case other => fail(s"int: expected IntegerSchema, got $other")
        }

        properties("long") match {
          case Schema.IntegerSchema(_) => // expected
          case other => fail(s"long: expected IntegerSchema, got $other")
        }

        properties("double") match {
          case Schema.NumberSchema(_) => // expected
          case other => fail(s"double: expected NumberSchema, got $other")
        }

        properties("float") match {
          case Schema.NumberSchema(_) => // expected
          case other => fail(s"float: expected NumberSchema, got $other")
        }

        properties("bool") match {
          case Schema.BooleanSchema(_) => // expected
          case other => fail(s"bool: expected BooleanSchema, got $other")
        }

        properties("bigDec") match {
          case Schema.NumberSchema(_) => // expected
          case other => fail(s"bigDec: expected NumberSchema, got $other")
        }
      case other => fail(s"Expected ObjectSchema, got $other")
    }
  }

  test("derive schema for case class with collections") {
    case class CollectionTypes(
      list: List[String],
      seq: Seq[Int],
      vector: Vector[Boolean],
      set: Set[Double]
    ) derives JsonSchema

    val schema = JsonSchema.of[CollectionTypes]

    schema match {
      case Schema.ObjectSchema(properties, _, _) =>
        properties("list") match {
          case Schema.ArraySchema(Schema.StringSchema(_), _) => // expected
          case other => fail(s"list: expected ArraySchema[String], got $other")
        }

        properties("seq") match {
          case Schema.ArraySchema(Schema.IntegerSchema(_), _) => // expected
          case other => fail(s"seq: expected ArraySchema[Int], got $other")
        }

        properties("vector") match {
          case Schema.ArraySchema(Schema.BooleanSchema(_), _) => // expected
          case other => fail(s"vector: expected ArraySchema[Boolean], got $other")
        }

        properties("set") match {
          case Schema.ArraySchema(Schema.NumberSchema(_), _) => // expected
          case other => fail(s"set: expected ArraySchema[Double], got $other")
        }
      case other => fail(s"Expected ObjectSchema, got $other")
    }
  }

  test("convert schema to JSON") {
    case class Simple(name: String, count: Int) derives JsonSchema

    val schema = JsonSchema.of[Simple]
    val json = JsonSchema.toJson(schema)

    json match {
      case JsonObject(fields) =>
        assertEquals(fields("type"), JsonString("object"))

        fields("properties") match {
          case JsonObject(props) =>
            assertEquals(props.size, 2)

            props("name") match {
              case JsonObject(nameFields) =>
                assertEquals(nameFields("type"), JsonString("string"))
              case other => fail(s"Expected object for name, got $other")
            }

            props("count") match {
              case JsonObject(countFields) =>
                assertEquals(countFields("type"), JsonString("integer"))
              case other => fail(s"Expected object for count, got $other")
            }
          case other => fail(s"Expected properties object, got $other")
        }

        fields("required") match {
          case JsonArray(req) =>
            assertEquals(req.toList, List(JsonString("name"), JsonString("count")))
          case other => fail(s"Expected required array, got $other")
        }
      case other => fail(s"Expected JsonObject, got $other")
    }
  }

  test("extension methods") {
    import JsonSchema.*

    case class Item(id: String, price: Double) derives JsonSchema

    val item = Item("abc", 99.99)

    // Test schema extension
    val itemSchema = item.schema
    assertEquals(itemSchema.isInstanceOf[Schema.ObjectSchema], true)

    // Test schemaJson extension
    val json = item.schemaJson
    assertEquals(json.isInstanceOf[JsonObject], true)

    // Test schemaJsonString extension
    val jsonStr = item.schemaJsonString
    assertEquals(jsonStr.contains("\"type\""), true)
    assertEquals(jsonStr.contains("\"object\""), true)
  }

  test("nested case classes") {
    case class Address(street: String, city: String) derives JsonSchema
    case class Person(name: String, address: Address) derives JsonSchema

    val schema = JsonSchema.of[Person]

    schema match {
      case Schema.ObjectSchema(properties, _, _) =>
        properties("address") match {
          case Schema.ObjectSchema(addrProps, _, _) =>
            assert(addrProps.contains("street"))
            assert(addrProps.contains("city"))
          case other => fail(s"Expected nested ObjectSchema, got $other")
        }
      case other => fail(s"Expected ObjectSchema, got $other")
    }
  }

  test("collection of nested objects") {
    case class Tag(name: String) derives JsonSchema
    case class Post(title: String, tags: List[Tag]) derives JsonSchema

    val schema = JsonSchema.of[Post]
    println(JsonSchema.toJson(schema))

    schema match {
      case Schema.ObjectSchema(properties, _, _) =>
        properties("tags") match {
          case Schema.ArraySchema(Schema.ObjectSchema(tagProps, _, _), _) =>
            assert(tagProps.contains("name"))
          case other => fail(s"Expected ArraySchema[ObjectSchema], got $other")
        }
      case other => fail(s"Expected ObjectSchema, got $other")
    }
  }
}
