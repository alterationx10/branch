package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.friday.{Json, JsonCodec, JsonDecoder, JsonEncoder}

case class OllamaResponse(
    model: String,
    created_at: String,
    message: OllamaMessage,
    done_reason: String,
    done: Boolean,
    total_duration: Option[Long] = None,
    load_duration: Option[Long] = None,
    prompt_eval_count: Option[Int] = None,
    prompt_eval_duration: Option[Long] = None,
    eval_count: Option[Int] = None,
    eval_duration: Option[Long] = None
) derives JsonCodec

case class OllamaMessage(
    role: String,
    content: String = "",
    thinking: Option[String] = None,
    tool_calls: Option[List[OllamaToolCall]] = None
) derives JsonCodec

case class OllamaToolCall(
    function: OllamaFunction
) derives JsonCodec

case class OllamaFunction(
    name: String,
    arguments: Json // Object format in Ollama response
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
