package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.friday.{Json, JsonCodec, JsonDecoder, JsonEncoder}

import scala.util.Try

//given JsonEncoder[Map[String, Double]] = (a: Map[String, Double]) =>
//  Json.JsonObject(a.map { case (k, v) => k -> Json.JsonNumber(v) })
//
//given JsonDecoder[Map[String, Double]] = (json: Json) =>
//  Try(json.objVal.map { case (k, v) => k -> v.numVal })

given JsonEncoder[Map[String, Int]] = (a: Map[String, Int]) =>
  Json.JsonObject(a.map { case (k, v) => k -> Json.JsonNumber(v) })

given JsonDecoder[Map[String, Int]] = (json: Json) =>
  Try(json.objVal.map { case (k, v) => k -> v.numVal.toInt })

case class OllamaRequest(
    model: String,
    messages: List[RequestMessage],
    tools: Option[List[Tool]] = None,
    tool_choice: Option[Json] = None,
    stream: Boolean = false,
    temperature: Option[Double] = None,
    top_p: Option[Double] = None,
    max_tokens: Option[Int] = None, // Deprecated in favor of max_completion_tokens
    max_completion_tokens: Option[Int] = None,
    presence_penalty: Option[Double] = None,
    frequency_penalty: Option[Double] = None,
    logit_bias: Option[Map[String, Int]] = None, // Fixed: should be Int not Double (token IDs to bias)
    user: Option[String] = None, // Deprecated in favor of safety_identifier and prompt_cache_key
    n: Option[Int] = None,
    stop: Option[Json] = None, // Can be string or array of strings
    response_format: Option[ResponseFormat] = None,
    seed: Option[Int] = None, // Deprecated
    modalities: Option[List[String]] = None, // Response modalities like ["text", "audio"]
    verbosity: Option[String] = None, // Verbosity level
    reasoning_effort: Option[String] = None, // For o-series models
    web_search_options: Option[Json] = None, // Web search configuration
    top_logprobs: Option[Int] = None, // Number of top log probabilities (0-20)
    audio: Option[AudioConfig] = None, // Audio output parameters
    store: Option[Boolean] = None, // Whether to store for model distillation
    logprobs: Option[Boolean] = None, // Whether to return log probabilities
    prediction: Option[Json] = None, // Predicted output configuration
    stream_options: Option[Json] = None, // Stream configuration options
    parallel_tool_calls: Option[Boolean] = None, // Whether to allow parallel tool calls
    function_call: Option[Json] = None, // Deprecated in favor of tool_choice
    functions: Option[List[Json]] = None // Deprecated in favor of tools
) derives JsonCodec

case class RequestMessage(
    role: String,                       // "system", "user", "assistant", "tool"
    content: Option[Json] = None,       // Can be string or array of content parts
    name: Option[String] = None,
    tool_calls: Option[List[RequestToolCall]] = None,
    tool_call_id: Option[String] = None // For tool response messages
) derives JsonCodec

case class RequestToolCall(
    id: String,
    `type`: String, // Should be "function"
    function: RequestFunction
) derives JsonCodec

case class RequestFunction(
    name: String,
    arguments: String // JSON string, not Map
) derives JsonCodec

case class Tool(
    `type`: String, // "function"
    function: FunctionDefinition
) derives JsonCodec

case class FunctionDefinition(
    name: String,
    description: Option[String] = None,
    parameters: Option[Json] = None, // JSON Schema object
    strict: Option[Boolean] = None
) derives JsonCodec

case class ResponseFormat(
    `type`: String, // "text", "json_object", "json_schema"
    json_schema: Option[JsonSchema] = None
) derives JsonCodec

case class JsonSchema(
    name: String,
    description: Option[String] = None,
    schema: Json,
    strict: Option[Boolean] = None
) derives JsonCodec

case class ContentPart(
    `type`: String, // "text", "image_url"
    text: Option[String] = None,
    image_url: Option[ImageUrl] = None
) derives JsonCodec

case class ImageUrl(
    url: String,
    detail: Option[String] = None // "low", "high", "auto"
) derives JsonCodec

case class AudioConfig(
    voice: String, // Voice ID like "alloy", "ash", "ballad", etc.
    format: String // Audio format like "wav", "mp3", "flac", "opus", "aac", "pcm16"
) derives JsonCodec
