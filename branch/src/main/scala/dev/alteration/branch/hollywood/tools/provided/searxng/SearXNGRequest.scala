package dev.alteration.branch.hollywood.tools.provided.searxng

import dev.alteration.branch.friday.JsonCodec

case class SearXNGRequest(
  q: String,                              // Required: search query
  categories: Option[String] = None,       // Comma-separated list of categories
  engines: Option[String] = None,          // Comma-separated list of engines
  language: Option[String] = None,         // Language code (e.g., "en", "fr")
  time_range: Option[String] = None,       // Time filter: "day", "month", "year"
  pageno: Option[Int] = None,              // Page number (default: 1)
  format: Option[String] = Some("json"),   // Output format: "json", "csv", "rss"
  safesearch: Option[Int] = None           // Safe search level: 0-2
) derives JsonCodec
