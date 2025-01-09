package dev.wishingtree.branch.mustachio

import dev.wishingtree.branch.friday.Json
import dev.wishingtree.branch.friday.Json.*

/** It's like a cache for your mustache templates.
  */
enum Stache {
  case Str(value: String)
  case Arr(value: List[Stache])
  case Obj(value: Map[String, Stache])
}

object Stache {

  def fromJson(json: Json): Stache = json match {
    case JsonNull          => Str("null")
    case JsonString(value) => Str(value)
    case JsonBool(value)   => Str(value.toString)
    case JsonNumber(value) =>
      // A hack for now to format as int
      if value == value.toInt then Str(value.toInt.toString)
      else Str(value.toString)
    case JsonObject(value) => Obj(value.view.mapValues(fromJson).toMap)
    case JsonArray(value)  => Arr(value.map(fromJson).toList)
  }

  def str(value: String): Stache.Str =
    Str(value)

  def context(fields: (String, Stache)*): Stache.Obj =
    Obj(fields.toMap)

  extension (s: Stache) {
    def ?(field: String): Option[Stache] = s match {
      case Obj(value) => value.get(field)
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
