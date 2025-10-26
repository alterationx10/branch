package dev.alteration.branch.gooey.modifiers

import dev.alteration.branch.gooey.layout.{Component, RenderedComponent}

import java.awt.Color

/** Sets the background color of a component.
  *
  * @param child
  *   The component to set the background for
  * @param color
  *   The background color
  */
case class BackgroundModifier(child: Component, color: Color)
    extends Component {

  override def render(): RenderedComponent = {
    val childRendered = child.render()
    val component     = childRendered.asJComponent

    // If the child component is transparent, wrap it in a panel
    if (color.getAlpha == 0) {
      component.setOpaque(false)
    } else {
      component.setOpaque(true)
      component.setBackground(color)
    }

    childRendered
  }

}
