package dev.alteration.branch.hollywood.tools.schema

import dev.alteration.branch.hollywood.tools.schema.PropertySchema

case class ParameterSchema(
    `type`: String = "object",
    properties: Map[String, PropertySchema],
    required: List[String]
)
