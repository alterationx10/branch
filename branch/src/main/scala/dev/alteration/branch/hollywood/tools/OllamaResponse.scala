package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.friday.Json.{JsonObject, JsonString}
import dev.alteration.branch.friday.{Json, JsonCodec, JsonDecoder, JsonEncoder}

import scala.util.Try

case class OllamaResponse(
    model: String,
    created_at: String,
    message: OllamaMessage,
    done_reason: String,
    done: Boolean,
    total_duration: Long,
    load_duration: Long,
    prompt_eval_count: Int,
    prompt_eval_duration: Long,
    eval_count: Int,
    eval_duration: Long
) derives JsonCodec

case class OllamaMessage(
    role: String,
    content: String,
    tool_calls: List[OllamaToolCall] = List.empty
) derives JsonCodec

case class OllamaToolCall(
    function: OllamaFunction
) derives JsonCodec

given JsonEncoder[Map[String, String]] = (a: Map[String, String]) =>
  JsonObject(a.map { case (k, v) => k -> JsonString(v) })
given JsonDecoder[Map[String, String]] = (json: Json) =>
  Try(json.objVal.map { case (k, v) => k -> v.strVal })

case class OllamaFunction(
    name: String,
    arguments: Map[String, String]
) derives JsonCodec
