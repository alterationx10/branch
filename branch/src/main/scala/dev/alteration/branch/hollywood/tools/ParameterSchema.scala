package dev.alteration.branch.hollywood.tools

case class ParameterSchema(
    `type`: String = "object",
    properties: Map[String, PropertySchema],
    required: List[String]
)
