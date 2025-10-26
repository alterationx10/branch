package dev.alteration.branch.gooey.modifiers

import dev.alteration.branch.gooey.layout.{Component, RenderedComponent}

import javax.swing.JPanel
import java.awt.{BorderLayout, Graphics, Graphics2D, RenderingHints}
import java.awt.geom.RoundRectangle2D

/** Adds rounded corners to a component.
  *
  * Note: This uses a custom painting approach to achieve rounded corners. The
  * radius applies to all corners equally.
  *
  * @param child
  *   The component to add rounded corners to
  * @param radius
  *   The corner radius in pixels
  */
case class CornerRadiusModifier(child: Component, radius: Int)
    extends Component {

  override def render(): RenderedComponent = {
    val childRendered = child.render()
    val innerComp     = childRendered.asJComponent

    val roundedPanel = new JPanel(new BorderLayout()) {
      override def paintComponent(g: Graphics): Unit = {
        val g2 = g.asInstanceOf[Graphics2D]
        g2.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON
        )

        // Draw rounded background
        g2.setColor(getBackground)
        g2.fill(
          new RoundRectangle2D.Float(
            0,
            0,
            getWidth,
            getHeight,
            radius * 2,
            radius * 2
          )
        )

        super.paintComponent(g)
      }

      override def paintBorder(g: Graphics): Unit = {
        // Custom border rendering for rounded corners if needed
        super.paintBorder(g)
      }
    }

    roundedPanel.setOpaque(false)
    roundedPanel.add(innerComp, BorderLayout.CENTER)
    RenderedComponent(roundedPanel)
  }

}
