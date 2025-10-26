package dev.alteration.branch.gooey.layout

import dev.alteration.branch.gooey.styling.Alignment

import javax.swing.{Box, BoxLayout, JPanel}

/** Horizontal stack layout container.
  *
  * Arranges child components horizontally with optional spacing and alignment.
  *
  * @param spacing
  *   Space between children in pixels (default: 0)
  * @param alignment
  *   Vertical alignment of children (default: Center)
  * @param children
  *   Child components to arrange horizontally
  */
case class HStack(
    spacing: Int = 0,
    alignment: Alignment = Alignment.Center,
    children: Component*
) extends Component {

  override def render(): RenderedComponent = {
    val panel = new JPanel()
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS))
    panel.setOpaque(false)

    children.zipWithIndex.foreach { case (child, idx) =>
      val rendered = child.render()
      val comp     = rendered.asJComponent

      // Set alignment
      comp.setAlignmentY(alignment.toAlignmentY)

      panel.add(comp)

      // Add spacing (except after last child)
      if (idx < children.size - 1 && spacing > 0) {
        panel.add(Box.createHorizontalStrut(spacing))
      }
    }

    RenderedComponent(panel)
  }

}
