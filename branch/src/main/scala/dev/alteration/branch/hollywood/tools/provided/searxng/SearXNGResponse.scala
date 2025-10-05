package dev.alteration.branch.hollywood.tools.provided.searxng

import dev.alteration.branch.friday.{Json, JsonCodec}

case class SearchResult(
  title: String,
  url: Option[String] = None,
  content: String,
  thumbnail: Option[String] = None,
  engine: String,
  template: String,
  parsed_url: Option[List[String]] = None,
  img_src: Option[String] = None,
  priority: Option[String] = None,
  engines: List[String],
  positions: Option[List[Int]] = None,
  score: Double,
  category: Option[String] = None,
  publishedDate: Option[String] = None,
  pubdate: Option[String] = None
) derives JsonCodec

case class SearXNGResponse(
  query: String,
  number_of_results: Int,
  results: List[SearchResult],
  answers: Option[List[Json]] = None,
  corrections: Option[List[String]] = None,
  infoboxes: Option[List[Json]] = None,
  suggestions: Option[List[String]] = None,
  unresponsive_engines: Option[List[String]] = None
) derives JsonCodec
