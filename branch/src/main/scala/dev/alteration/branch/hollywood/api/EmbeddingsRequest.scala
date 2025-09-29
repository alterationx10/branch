package dev.alteration.branch.hollywood.api

import dev.alteration.branch.friday.{Json, JsonCodec}

case class EmbeddingsRequest(
    input: Json,                            // Can be string or array of strings
    model: String,
    encoding_format: Option[String] = None, // "float", "base64"
    dimensions: Option[Int] = None,
    user: Option[String] = None
) derives JsonCodec
