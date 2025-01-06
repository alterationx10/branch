package dev.wishingtree.branch.mustachio

import dev.wishingtree.branch.friday.Json
import dev.wishingtree.branch.friday.Json.*

/** It's like a cache for your mustache templates.
  */
enum Stache {
  case Str(value: String)
  case Context(value: Map[String, Stache])
}

object Stache {

  def fromJson(json: Json): Stache = json match {
    case JsonNull => Str("")
    case JsonString(value) => Str(value)
    case JsonBool(value) => Str(value.toString)
    case JsonNumber(value) => Str(value.toString) // Formatting
    case JsonObject(value) => Context(value.view.mapValues(fromJson).toMap)
    case JsonArray(value) => Str("") // wut do?
  }
  
  def str(value: String): Stache.Str =
    Str(value)

  def context(fields: (String, Stache)*): Stache.Context =
    Context(fields.toMap)

  extension (s: Stache) {
    def ?(field: String): Option[Stache] = s match {
      case Context(value) => value.get(field)
      case _              => None
    }

    def strVal: String = s match {
      case Str(value) => value
      case _          => ""
    }
  }

  extension (s: Option[Stache]) {
    def ?(field: String): Option[Stache] =
      s.flatMap(_ ? field)
  }
}
