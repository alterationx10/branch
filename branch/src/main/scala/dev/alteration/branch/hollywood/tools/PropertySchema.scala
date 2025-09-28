package dev.alteration.branch.hollywood.tools

case class PropertySchema(
    `type`: String,
    description: String,
    enumValues: Option[List[String]] = None
)
