package dev.alteration.branch.hollywood.api

import dev.alteration.branch.friday.{Json, JsonCodec, JsonDecoder, JsonEncoder}

import scala.util.Try

given JsonEncoder[Map[String, Double]] = (a: Map[String, Double]) =>
  Json.JsonObject(a.map { case (k, v) => k -> Json.JsonNumber(v) })

given JsonDecoder[Map[String, Double]] = (json: Json) =>
  Try(json.objVal.map { case (k, v) => k -> v.numVal })

case class ChatCompletionsRequest(
    messages: List[ChatMessage],
    model: String,
    frequency_penalty: Option[Double] = None,
    logit_bias: Option[Map[String, Double]] = None,
    logprobs: Option[Boolean] = None,
    top_logprobs: Option[Int] = None,
    max_tokens: Option[Int] = None,
    n: Option[Int] = None,
    presence_penalty: Option[Double] = None,
    response_format: Option[ResponseFormat] = None,
    seed: Option[Int] = None,
    service_tier: Option[String] = None,
    stop: Option[Json] = None,        // Can be string or array of strings
    stream: Option[Boolean] = None,
    stream_options: Option[StreamOptions] = None,
    temperature: Option[Double] = None,
    tool_choice: Option[Json] = None, // Can be string or ToolChoice object
    tools: Option[List[Tool]] = None,
    top_p: Option[Double] = None,
    user: Option[String] = None,
    // llama.cpp specific parameters
    chat_template_kwargs: Option[Json] = None,
    reasoning_format: Option[String] = None,
    parse_tool_calls: Option[Boolean] = None
) derives JsonCodec

case class ChatMessage(
    role: String,                            // "system", "user", "assistant", "tool"
    content: Option[String] = None,          // Can be string or array of ContentPart
    name: Option[String] = None,
    tool_calls: Option[List[ToolCall]] = None,
    tool_call_id: Option[String] = None,
    reasoning_content: Option[String] = None // llama.cpp reasoning content
) derives JsonCodec

case class ContentPart(
    `type`: String, // "text" or "image_url"
    text: Option[String] = None,
    image_url: Option[ImageUrl] = None
) derives JsonCodec

case class ImageUrl(
    url: String,
    detail: Option[String] = None // "auto", "low", "high"
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

case class ToolCall(
    id: String,
    `type`: String, // "function"
    function: FunctionCall
) derives JsonCodec

case class FunctionCall(
    name: String,
    arguments: String // JSON string
) derives JsonCodec {
  def argumentMap: Map[String, String] =
    Json
      .parse(arguments.translateEscapes())
      .map(_.objVal.map { case (k, v) => k -> v.toString })
      .getOrElse(Map.empty)
}

case class ToolChoice(
    `type`: String, // "function"
    function: ToolChoiceFunction
) derives JsonCodec

case class ToolChoiceFunction(
    name: String
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

case class StreamOptions(
    include_usage: Option[Boolean] = None
) derives JsonCodec
