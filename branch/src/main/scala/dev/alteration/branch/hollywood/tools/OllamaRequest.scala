package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.friday.{Json, JsonCodec}

case class OllamaRequest(
    model: String,
    messages: List[RequestMessage],
    tools: Json = Json.JsonArray(IndexedSeq.empty),
    stream: Boolean = false
) derives JsonCodec

case class RequestMessage(
    role: String,
    content: String,
    tool_calls: List[RequestToolCall] = List.empty
) derives JsonCodec

case class RequestToolCall(
    function: RequestFunction
) derives JsonCodec

case class RequestFunction(
    name: String,
    arguments: Map[String, String]
) derives JsonCodec