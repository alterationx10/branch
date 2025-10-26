package dev.alteration.branch.gooey.layout

/** Base trait for all layout components in the Gooey DSL.
  *
  * Components are immutable and composable. Modifiers return new component
  * instances, allowing for a fluent builder-style API.
  */
trait Component {

  /** Renders this component to a Swing JComponent.
    *
    * @return
    *   A RenderedComponent wrapping the Swing component
    */
  def render(): RenderedComponent

}
