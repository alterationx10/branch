package dev.alteration.branch.gooey.modifiers

import dev.alteration.branch.gooey.layout.{Component, RenderedComponent}

import javax.swing.{BorderFactory, JPanel}
import java.awt.BorderLayout

/** Adds padding around a component.
  *
  * @param child
  *   The component to add padding to
  * @param top
  *   Top padding in pixels
  * @param right
  *   Right padding in pixels
  * @param bottom
  *   Bottom padding in pixels
  * @param left
  *   Left padding in pixels
  */
case class PaddingModifier(
    child: Component,
    top: Int,
    right: Int,
    bottom: Int,
    left: Int
) extends Component {

  override def render(): RenderedComponent = {
    val childRendered = child.render()
    val panel         = new JPanel(new BorderLayout())
    panel.setOpaque(false)
    panel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right))
    panel.add(childRendered.asJComponent, BorderLayout.CENTER)
    RenderedComponent(panel)
  }

}
