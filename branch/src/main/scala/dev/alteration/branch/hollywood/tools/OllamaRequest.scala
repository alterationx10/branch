package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.friday.{Json, JsonCodec, JsonDecoder, JsonEncoder}


case class OllamaRequest(
    model: String,
    messages: List[RequestMessage],
    tools: Option[List[Tool]] = None,
    stream: Boolean = false,
    temperature: Option[Double] = None,
    top_p: Option[Double] = None,
    max_tokens: Option[Int] = None,
    stop: Option[Json] = None, // Can be string or array of strings
    seed: Option[Int] = None
) derives JsonCodec

case class RequestMessage(
    role: String,                       // "system", "user", "assistant", "tool"
    content: Option[String] = None,     // Simplified to string for Ollama
    tool_calls: Option[List[RequestToolCall]] = None,
    tool_call_id: Option[String] = None // For tool response messages
) derives JsonCodec

case class RequestToolCall(
    function: RequestFunction // Simplified - no id or type for Ollama
) derives JsonCodec

case class RequestFunction(
    name: String,
    arguments: Json // JSON object for Ollama, not string
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
