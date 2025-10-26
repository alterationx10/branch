package dev.alteration.branch.gooey.layout

import javax.swing.JComponent

/** Wraps a rendered Swing JComponent.
  *
  * This provides a type-safe way to work with rendered components while
  * maintaining the distinction between DSL components and their Swing
  * representations.
  *
  * @param component
  *   The underlying Swing component
  */
case class RenderedComponent(component: JComponent) {

  /** Gets the underlying Swing component. */
  def asJComponent: JComponent = component

}

object RenderedComponent {

  /** Creates a RenderedComponent from a JComponent. */
  def apply(component: JComponent): RenderedComponent =
    new RenderedComponent(component)

}
