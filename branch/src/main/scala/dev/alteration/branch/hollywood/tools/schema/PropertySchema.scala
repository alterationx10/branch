package dev.alteration.branch.hollywood.tools.schema

case class PropertySchema(
    `type`: String,
    description: String,
    enumValues: Option[List[String]] = None
)
