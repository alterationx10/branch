package dev.alteration.branch.hollywood.tools

trait Tool[A] extends Product {
  def execute(): A
}
