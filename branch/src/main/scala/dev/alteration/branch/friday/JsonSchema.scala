package dev.alteration.branch.friday

import dev.alteration.branch.macaroni.meta.Summons.summonHigherListOf

import scala.compiletime.*
import scala.deriving.Mirror

/** Represents a JSON Schema for a type */
sealed trait Schema

object Schema {

  case class StringSchema(description: Option[String] = None)
      extends Schema
  case class NumberSchema(description: Option[String] = None)
      extends Schema
  case class IntegerSchema(description: Option[String] = None)
      extends Schema
  case class BooleanSchema(description: Option[String] = None)
      extends Schema
  case class ArraySchema(
      items: Schema,
      description: Option[String] = None
  ) extends Schema
  case class ObjectSchema(
      properties: Map[String, Schema],
      required: List[String],
      description: Option[String] = None
  ) extends Schema
  case class EnumSchema(
      values: List[String],
      description: Option[String] = None
  ) extends Schema
}

/** Type class for deriving JSON Schema from a type */
trait JsonSchema[A] {
  def schema: Schema
}

object JsonSchema {

  import Schema.*

  def apply[A](using js: JsonSchema[A]): JsonSchema[A] = js

    // Primitive types
    given JsonSchema[String] with {
      def schema: Schema = StringSchema()
    }

    given JsonSchema[Int] with {
      def schema: Schema = IntegerSchema()
    }

    given JsonSchema[Long] with {
      def schema: Schema = IntegerSchema()
    }

    given JsonSchema[Double] with {
      def schema: Schema = NumberSchema()
    }

    given JsonSchema[Float] with {
      def schema: Schema = NumberSchema()
    }

    given JsonSchema[BigDecimal] with {
      def schema: Schema = NumberSchema()
    }

    given JsonSchema[Boolean] with {
      def schema: Schema = BooleanSchema()
    }

    // Option types - unwrap to the inner schema
    given [A](using inner: JsonSchema[A]): JsonSchema[Option[A]]
    with {
      def schema: Schema = inner.schema
    }

    // Collection types
    given [A](using inner: JsonSchema[A]): JsonSchema[List[A]]
    with {
      def schema: Schema = ArraySchema(inner.schema)
    }

    given [A](using inner: JsonSchema[A]): JsonSchema[Seq[A]] with {
      def schema: Schema = ArraySchema(inner.schema)
    }

    given [A](using
        inner: JsonSchema[A]
    ): JsonSchema[IndexedSeq[A]]
    with {
      def schema: Schema = ArraySchema(inner.schema)
    }

    given [A](using inner: JsonSchema[A]): JsonSchema[Vector[A]]
    with {
      def schema: Schema = ArraySchema(inner.schema)
    }

    given [A](using inner: JsonSchema[A]): JsonSchema[Set[A]]
    with {
      def schema: Schema = ArraySchema(inner.schema)
    }

    // Derive for product types (case classes)
    protected class DerivedObjectSchema[A](
        labels: List[String],
        schemas: List[JsonSchema[?]],
        optionalFlags: List[Boolean]
    ) extends JsonSchema[A] {
      def schema: Schema = {
        val properties = labels
          .zip(schemas)
          .map { case (label, schema) =>
            label -> schema.schema
          }
          .toMap

        // Only non-Optional fields are required
        val required = labels
          .zip(optionalFlags)
          .filterNot(_._2)
          .map(_._1)

        ObjectSchema(properties, required)
      }
    }

    private inline def labelsAsList[T <: Tuple]: List[String] = {
      inline erasedValue[T] match {
        case _: EmptyTuple => Nil
        case _: (t *: ts)  =>
          constValue[t].toString :: labelsAsList[ts]
      }
    }

    private inline def isOptionList[T <: Tuple]: List[Boolean] = {
      inline erasedValue[T] match {
        case _: EmptyTuple => Nil
        case _: (t *: ts)  =>
          isOptionType[t] :: isOptionList[ts]
      }
    }

    private inline def isOptionType[T]: Boolean = {
      inline erasedValue[T] match {
        case _: Option[?] => true
        case _            => false
      }
    }

  inline given derived[A](using m: Mirror.Of[A]): JsonSchema[A] = {
    inline m match {
      case _: Mirror.SumOf[A] =>
        error("Auto derivation of Sum types is not currently supported")
      case p: Mirror.ProductOf[A] =>
        val labels = labelsAsList[p.MirroredElemLabels]
        val schemas = summonHigherListOf[p.MirroredElemTypes, JsonSchema]
        val optionals = isOptionList[p.MirroredElemTypes]
        new DerivedObjectSchema[A](labels, schemas, optionals)
    }
  }

  /** Get the schema for a type */
  inline def of[A](using js: JsonSchema[A]): Schema = js.schema

  /** Convert a schema to JSON format */
  def toJson(schema: Schema): Json = {
    import Schema.*
    schema match {
      case StringSchema(desc) =>
        Json.JsonObject(
          Map("type" -> Json.JsonString("string")) ++ desc
            .map(d => "description" -> Json.JsonString(d))
        )

      case NumberSchema(desc) =>
        Json.JsonObject(
          Map("type" -> Json.JsonString("number")) ++ desc
            .map(d => "description" -> Json.JsonString(d))
        )

      case IntegerSchema(desc) =>
        Json.JsonObject(
          Map("type" -> Json.JsonString("integer")) ++ desc
            .map(d => "description" -> Json.JsonString(d))
        )

      case BooleanSchema(desc) =>
        Json.JsonObject(
          Map("type" -> Json.JsonString("boolean")) ++ desc
            .map(d => "description" -> Json.JsonString(d))
        )

      case ArraySchema(items, desc) =>
        Json.JsonObject(
          Map(
            "type"  -> Json.JsonString("array"),
            "items" -> toJson(items)
          ) ++ desc.map(d => "description" -> Json.JsonString(d))
        )

      case ObjectSchema(properties, required, desc) =>
        Json.JsonObject(
          Map(
            "type"       -> Json.JsonString("object"),
            "properties" -> Json.JsonObject(properties.view.mapValues(toJson).toMap),
            "required"   -> Json.JsonArray(
              required.map(Json.JsonString(_)).toIndexedSeq
            )
          ) ++ desc.map(d => "description" -> Json.JsonString(d))
        )

      case EnumSchema(values, desc) =>
        Json.JsonObject(
          Map(
            "type" -> Json.JsonString("string"),
            "enum" -> Json.JsonArray(
              values.map(Json.JsonString(_)).toIndexedSeq
            )
          ) ++ desc.map(d => "description" -> Json.JsonString(d))
        )
    }
  }

  /** Extension methods for types with schema derivation */
  extension [A](a: A)(using js: JsonSchema[A]) {
    def schema: Schema           = js.schema
    def schemaJson: Json         = toJson(js.schema)
    def schemaJsonString: String = toJson(js.schema).toJsonString
  }
}
