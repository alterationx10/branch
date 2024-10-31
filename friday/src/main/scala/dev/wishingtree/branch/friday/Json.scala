package dev.wishingtree.branch.friday

enum Json:
  case JsonNull
  case JsonBool(value: Boolean)
  case JsonNumber(value: Double)
  case JsonString(value: String)
  case JsonArray(value: IndexedSeq[Json])
  case JsonObject(value: Map[String, Json])

object Json {

  def parser[Parser[+_]](P: Parsers[Parser]): Parser[Json] = {
    import P.*

    def token(s: String) = string(s).token

    def keyVal: Parser[(String, Json)] =
      escapedQuoted ** (token(":") *> value)

    def obj: Parser[Json] = {
      token("{") *> keyVal.sep(token(",")).map { kvs =>
        JsonObject(kvs.toMap)
      } <* token("}")
    }.scope("object")

    def array: Parser[Json] = {
      token("[") *>
        value.sep(token(",")).map(vs => JsonArray(vs.toIndexedSeq))
        <* token("]")
    }.scope("array")

    def literal: Parser[Json] = {
      token("null").as(JsonNull) |
        double.map(JsonNumber.apply) |
        escapedQuoted.map(JsonString.apply) |
        token("true").as(JsonBool(true)) |
        token("false").as(JsonBool(false))
    }

    def value: Parser[Json] = literal | obj | array

    (whitespace *> (obj | array)).root
  }

}
