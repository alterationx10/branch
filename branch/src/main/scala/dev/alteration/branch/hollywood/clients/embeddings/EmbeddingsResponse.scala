package dev.alteration.branch.hollywood.clients.embeddings

import dev.alteration.branch.friday.JsonCodec

case class EmbeddingsResponse(
    `object`: String, // "list"
    data: List[Embedding],
    model: String,
    usage: EmbeddingUsage
) derives JsonCodec

case class Embedding(
    `object`: String,        // "embedding"
    embedding: List[Double], // The embedding vector
    index: Int
) derives JsonCodec

case class EmbeddingUsage(
    prompt_tokens: Int,
    total_tokens: Int
) derives JsonCodec
