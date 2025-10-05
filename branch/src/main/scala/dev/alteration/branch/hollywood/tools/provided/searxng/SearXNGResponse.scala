package dev.alteration.branch.hollywood.tools.provided.searxng

import dev.alteration.branch.friday.JsonCodec

case class SearchResult(
    title: String,
    url: Option[String] = None,
    content: String,
    engine: String,
    score: Option[Double] = None,
    publishedDate: Option[String] = None
) derives JsonCodec

case class SearXNGResponse(
    query: String,
    results: List[SearchResult]
) derives JsonCodec
