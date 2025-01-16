package dev.wishingtree.branch.mustachio

import dev.wishingtree.branch.friday.Json
import dev.wishingtree.branch.friday.Json.*

/** It's like a cache for your mustache templates.
  */
enum Stache {

  /** A string value.
    */
  case Str(value: String)

  /** An array of staches.
    */
  case Arr(value: List[Stache])

  /** An object of staches.
    */
  case Obj(value: Map[String, Stache])

  /** A null value.
    */
  case Null
}

object Stache {

  /** An empty Stache.Obj
    */
  def empty: Stache =
    Stache.Obj(Map.empty)

  extension (s: String) {

    /** A String extension method to unescape some special characters, that
      * would normally be escaped when reading from a file.
      *
      *   - \n
      *   - \r
      *   - \t
      */
    def unescape: String =
      s
        .replaceAll("\\\\n", "\n")
        .replaceAll("\\\\r", "\r")
        .replaceAll("\\\\t", "\t")
  }

  /** Convert Json to a Stache representation.
    *
    * Note that number fields are tested for int equality and formatted as int
    * if so.
    *
    * @param json
    * @return
    */
  def fromJson(json: Json): Stache = json match {
    case JsonNull          => Null
    case JsonString(value) => Str(value.unescape)
    case JsonBool(value)   => Str(value.toString)
    case JsonNumber(value) =>
      // A hack for now to format as int
      if value == value.toInt then Str(value.toInt.toString)
      else Str(value.toString)
    case JsonObject(value) => Obj(value.view.mapValues(fromJson).toMap)
    case JsonArray(value)  => Arr(value.map(fromJson).toList)
  }

  /** Helper method for constructing Stash.Str */
  def str(value: String): Stache.Str =
    Str(value)

  /** Helper method for constructing Stash.Obj */
  def obj(fields: (String, Stache)*): Stache.Obj =
    Obj(fields.toMap)

  extension (s: Stache) {

    /** Get a field from a Stache object, if present.
      *
      * This will parse ".", so you can for example pass "a.b.c" to access the
      * field "c" of object b in object a.
      *
      * Passing in "." will return the Stache object itself.
      */
    def ?(field: String): Option[Stache] = {
      // Early exit if the field is a dot
      if field == "." then return Some(s)

      val fields = field.trim.split("\\.")
      fields
        .foldLeft(Option(s))((s, field) => {
          s match {
            case Some(Obj(value)) => value.get(field)
            case _                => None
          }
        })
    }

    /** Convert the Stache to a nice String representation */
    def toPrettyString: String = s match {
      case Null       => "null"
      case Str(value) => s"\"$value\""
      case Arr(value) =>
        "[" + value.map(_.toPrettyString).mkString(", ") + "]"
      case Obj(value) =>
        "{" + value
          .map { case (k, v) => "\"" + k + "\": " + v.toPrettyString }
          .mkString(", ") + "}"
    }

    /** Print the Stache to the console via the [[toPrettyString]] method. */
    def prettyPrint(): Unit = println(toPrettyString)

    /** Get a String representation of a Stache.
      *
      * Note, anything that's not a Stache.Str default to an empty String
      */
    def strVal: String = s match {
      case Str(value) => value
      case Null       => ""
      case _          => ""
    }
  }

  extension (s: Option[Stache]) {

    /** Get a field from a Stache object, if present.
      *
      * This will parse ".", so you can for example pass "a.b.c" to access the
      * field "c" of object b in object a.
      *
      * Passing in "." will return the Stache object itself.
      */
    def ?(field: String): Option[Stache] =
      s.flatMap(_ ? field)
  }
}
