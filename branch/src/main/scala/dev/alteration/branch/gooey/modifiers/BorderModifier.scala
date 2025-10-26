package dev.alteration.branch.gooey.modifiers

import dev.alteration.branch.gooey.layout.{Component, RenderedComponent}

import java.awt.Color
import javax.swing.BorderFactory

/** Adds a border around a component.
  *
  * @param child
  *   The component to add a border to
  * @param color
  *   The border color
  * @param width
  *   The border width in pixels
  */
case class BorderModifier(child: Component, color: Color, width: Int)
    extends Component {

  override def render(): RenderedComponent = {
    val childRendered = child.render()
    val component     = childRendered.asJComponent

    val existingBorder = component.getBorder
    val lineBorder     = BorderFactory.createLineBorder(color, width)

    if (existingBorder != null) {
      component.setBorder(
        BorderFactory.createCompoundBorder(lineBorder, existingBorder)
      )
    } else {
      component.setBorder(lineBorder)
    }

    childRendered
  }

}
