package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.friday.Json.{JsonObject, JsonString}
import dev.alteration.branch.friday.{Json, JsonCodec, JsonDecoder, JsonEncoder}

import scala.util.Try

case class OllamaResponse(
    id: String,
    `object`: String = "chat.completion",
    created: Long,
    model: String,
    choices: List[OllamaChoice],
    usage: Option[OllamaUsage] = None,
    system_fingerprint: Option[String] = None
) derives JsonCodec

case class OllamaChoice(
    index: Int,
    message: OllamaMessage,
    logprobs: Option[OllamaLogprobs] = None,
    finish_reason: Option[String] = None
) derives JsonCodec

case class OllamaMessage(
    role: String,
    content: Option[String] = None,
    tool_calls: Option[List[OllamaToolCall]] = None
) derives JsonCodec

case class OllamaToolCall(
    id: String,
    `type`: String = "function",
    function: OllamaFunction
) derives JsonCodec

case class OllamaFunction(
    name: String,
    arguments: String
) derives JsonCodec

case class OllamaUsage(
    prompt_tokens: Int,
    completion_tokens: Int,
    total_tokens: Int
) derives JsonCodec

case class OllamaLogprobs(
    content: Option[List[OllamaTokenLogprob]] = None
) derives JsonCodec

case class OllamaTokenLogprob(
    token: String,
    logprob: Double,
    bytes: Option[List[Int]] = None,
    top_logprobs: Option[List[OllamaTopLogprob]] = None
) derives JsonCodec

case class OllamaTopLogprob(
    token: String,
    logprob: Double,
    bytes: Option[List[Int]] = None
) derives JsonCodec
