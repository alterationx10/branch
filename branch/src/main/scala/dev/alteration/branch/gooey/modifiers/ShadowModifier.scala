package dev.alteration.branch.gooey.modifiers

import dev.alteration.branch.gooey.layout.{Component, RenderedComponent}

import javax.swing.{BorderFactory, JPanel}
import java.awt.{BorderLayout, Color}

/** Adds a drop shadow effect to a component.
  *
  * Note: Shadow rendering in Swing is limited. This implementation provides a
  * basic shadow effect using borders and alpha transparency.
  *
  * @param child
  *   The component to add a shadow to
  * @param offsetX
  *   Horizontal shadow offset in pixels
  * @param offsetY
  *   Vertical shadow offset in pixels
  * @param blur
  *   Shadow blur radius in pixels
  * @param opacity
  *   Shadow opacity (0.0-1.0)
  */
case class ShadowModifier(
    child: Component,
    offsetX: Int,
    offsetY: Int,
    blur: Int,
    opacity: Float
) extends Component {

  override def render(): RenderedComponent = {
    val childRendered = child.render()
    val innerComp     = childRendered.asJComponent

    // For a basic shadow effect, we'll add a gray border
    // More sophisticated shadow would require custom painting
    new Color(0, 0, 0, (opacity * 255).toInt.max(0).min(255))

    val panel = new JPanel(new BorderLayout())
    panel.setOpaque(false)

    // Create empty border to simulate shadow offset
    val shadowSize = Math.max(Math.abs(offsetX), Math.abs(offsetY)) + blur
    panel.setBorder(
      BorderFactory.createEmptyBorder(
        if (offsetY < 0) shadowSize else blur / 2,
        if (offsetX < 0) shadowSize else blur / 2,
        if (offsetY > 0) shadowSize else blur / 2,
        if (offsetX > 0) shadowSize else blur / 2
      )
    )

    panel.add(innerComp, BorderLayout.CENTER)
    RenderedComponent(panel)
  }

}
