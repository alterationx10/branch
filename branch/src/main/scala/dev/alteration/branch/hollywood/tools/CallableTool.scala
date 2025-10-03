package dev.alteration.branch.hollywood.tools

trait CallableTool[A] extends Product {
  def execute(): A
}
